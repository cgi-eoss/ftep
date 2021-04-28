package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.clouds.service.Node;
import com.cgi.eoss.ftep.clouds.service.NodeProvisioningException;
import com.cgi.eoss.ftep.clouds.service.StorageProvisioningException;
import com.cgi.eoss.ftep.io.ServiceInputOutputManager;
import com.cgi.eoss.ftep.io.ServiceIoException;
import com.cgi.eoss.ftep.logging.Logging;
import com.cgi.eoss.ftep.rpc.FileStream;
import com.cgi.eoss.ftep.rpc.FileStreamIOException;
import com.cgi.eoss.ftep.rpc.FileStreamServer;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.worker.Binding;
import com.cgi.eoss.ftep.rpc.worker.CleanUpResponse;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.ContainerStatus;
import com.cgi.eoss.ftep.rpc.worker.DockerImageConfig;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.GetJobEnvironmentRequest;
import com.cgi.eoss.ftep.rpc.worker.GetNodesRequest;
import com.cgi.eoss.ftep.rpc.worker.GetNodesResponse;
import com.cgi.eoss.ftep.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.ftep.rpc.worker.GetResumableJobsRequest;
import com.cgi.eoss.ftep.rpc.worker.GetResumableJobsResponse;
import com.cgi.eoss.ftep.rpc.worker.JobContainer;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import com.cgi.eoss.ftep.rpc.worker.LaunchContainerResponse;
import com.cgi.eoss.ftep.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.ftep.rpc.worker.OutputFileItem;
import com.cgi.eoss.ftep.rpc.worker.OutputFileList;
import com.cgi.eoss.ftep.rpc.worker.PortBinding;
import com.cgi.eoss.ftep.rpc.worker.PortBindings;
import com.cgi.eoss.ftep.rpc.worker.PrepareDockerImageResponse;
import com.cgi.eoss.ftep.rpc.worker.ResourceRequest;
import com.cgi.eoss.ftep.rpc.worker.StopContainerResponse;
import com.cgi.eoss.ftep.rpc.worker.TerminateJobRequest;
import com.cgi.eoss.ftep.worker.DockerRegistryConfig;
import com.cgi.eoss.ftep.worker.docker.Log4jContainerCallback;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.io.MoreFiles;
import com.google.common.util.concurrent.Striped;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.jooq.lambda.Unchecked;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * <p>Service for executing F-TEP (WPS) services inside Docker containers.</p>
 */
@GRpcService
@Log4j2
public class FtepWorker extends FtepWorkerGrpc.FtepWorkerImplBase {

    private final FtepWorkerNodeManager nodeManager;
    private final JobEnvironmentService jobEnvironmentService;
    private final ServiceInputOutputManager inputOutputManager;
    private final FtepDockerService ftepDockerService;
    private final boolean keepProcDir;

    private DockerRegistryConfig dockerRegistryConfig;

    // Track all nodes
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    // Track which Node is used for each job
    private final Map<String, Node> jobNodes = new ConcurrentHashMap<>();
    // Track which JobEnvironment is used for each job
    private final Map<String, com.cgi.eoss.ftep.worker.worker.JobEnvironment> jobEnvironments = new ConcurrentHashMap<>();
    // Track which DockerClient is used for each job
    private final Map<String, DockerClient> jobClients = new ConcurrentHashMap<>();
    // Track which container ID is used for each job
    private final Map<String, String> jobContainers = new ConcurrentHashMap<>();
    // Track which input URIs are used for each job
    private final SetMultimap<String, URI> jobInputs = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    private final Striped<Lock> dockerBuildLock = Striped.lazyWeakLock(1);

    private final SetMultimap<String, Path> externalInputs = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    private static final String DOCKER_HOST_URL = "unix:///var/run/docker.sock";
    public static final String JOB_ID = "jobId";
    public static final String INT_JOB_ID = "intJobId";
    public static final String USER_ID = "userId";
    public static final String SERVICE_ID = "serviceId";
    public static final String WORKER_ID = "workerId";
    public static final String TIMEOUT_VALUE = "timeoutValue";

    @Autowired
    public FtepWorker(FtepWorkerNodeManager nodeManager, JobEnvironmentService jobEnvironmentService,
                      ServiceInputOutputManager inputOutputManager, FtepDockerService ftepDockerService,
                      @Qualifier("keepProcDir") boolean keepProcDir) {
        this.nodeManager = nodeManager;
        this.jobEnvironmentService = jobEnvironmentService;
        this.inputOutputManager = inputOutputManager;
        this.ftepDockerService = ftepDockerService;
        this.keepProcDir = keepProcDir;
    }

    @Autowired(required = false)
    public void setDockerRegistryConfig(DockerRegistryConfig dockerRegistryConfig) {
        this.dockerRegistryConfig = dockerRegistryConfig;
    }

    private static CloseableThreadContext.Instance getJobLoggingContext(Job job) {
        return CloseableThreadContext.push("F-TEP Worker")
                .put("zooId", job.getId())
                .put(JOB_ID, String.valueOf(job.getIntJobId()))
                .put(USER_ID, job.getUserId())
                .put(SERVICE_ID, job.getServiceId());
    }

    @Override
    public void prepareInputs(JobInputs request, StreamObserver<JobEnvironment> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            String jobId = request.getJob().getId();

            Node node;
            try {
                node = prepareNode(jobId);
            } catch (NodeProvisioningException e) {
                LOG.error("Failed to prepare Node for {}", jobId, e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
                return;
            }

            try {
                prepareDockerClient(jobId, node);
            } catch (Exception e) {
                Logging.withUserLoggingContext(() -> LOG.error("Failed to prepare Docker context: {}", e.getMessage()));
                LOG.error("Failed to prepare Docker context for {}", jobId, e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
                return;
            }

            try {
                JobEnvironment jobEnvironment = prepareJobEnvironment(request, jobId);
                responseObserver.onNext(jobEnvironment);
                responseObserver.onCompleted();
            } catch (Exception e) {
                Logging.withUserLoggingContext(() -> LOG.error("Failed to prepare job inputs: {}", e.getMessage()));
                LOG.error("Failed to prepare job inputs for {}", jobId, e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    @Override
    public void launchContainer(JobSpec request, StreamObserver<LaunchContainerResponse> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            String jobId = request.getJob().getId();
            Preconditions.checkArgument(jobClients.containsKey(jobId), "Job ID %s is not attached to a DockerClient", jobId);

            DockerClient dockerClient = jobClients.get(jobId);
            String containerId = null;

            try {
                containerId = createContainer(request, dockerClient);
                jobContainers.put(jobId, containerId);

                LOG.info("Launching container {} for job {}", containerId, jobId);
                dockerClient.startContainerCmd(containerId).exec();

                dockerClient.logContainerCmd(containerId).withStdErr(true).withStdOut(true).withFollowStream(true).withTailAll()
                        .exec(new Log4jContainerCallback());

                responseObserver.onNext(LaunchContainerResponse.getDefaultInstance());
                responseObserver.onCompleted();
            } catch (Exception e) {
                Logging.withUserLoggingContext(() -> LOG.error("Failed to launch Docker container: {}", e.getMessage()));
                LOG.error("Failed to launch Docker container {}", request.getService().getDockerImageTag(), e);
                if (!Strings.isNullOrEmpty(containerId)) {
                    ftepDockerService.removeContainer(dockerClient, containerId);
                }
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    @Override
    public void getPortBindings(Job request, StreamObserver<PortBindings> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request)) {
            checkPreconditions(request.getId());
            DockerClient dockerClient = jobClients.get(request.getId());
            String containerId = jobContainers.get(request.getId());
            try {
                PortBindings portBindings = getBindings(dockerClient, containerId);
                responseObserver.onNext(portBindings);
                responseObserver.onCompleted();
            } catch (Exception e) {
                if (!Strings.isNullOrEmpty(containerId)) {
                    ftepDockerService.removeContainer(dockerClient, containerId);
                }
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    @Override
    public void stopContainer(Job request, StreamObserver<StopContainerResponse> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request)) {
            checkPreconditions(request.getId());
            DockerClient dockerClient = jobClients.get(request.getId());
            String containerId = jobContainers.get(request.getId());
            LOG.info("Stop requested for job {} running in container {}", request.getId(), containerId);

            try {
                stopContainer(dockerClient, containerId);
                responseObserver.onNext(StopContainerResponse.getDefaultInstance());
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Failed to stop job: {}", request.getId(), e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    @Override
    public void waitForContainerExit(ExitParams request, StreamObserver<ContainerExitCode> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            String jobId = request.getJob().getId();
            checkPreconditions(jobId);
            DockerClient dockerClient = jobClients.get(jobId);
            String containerId = jobContainers.get(jobId);
            try {
                int exitCode = waitForContainer(dockerClient, containerId).awaitStatusCode();
                LOG.info("Received exit code from container {}: {}", containerId, exitCode);
                responseObserver.onNext(ContainerExitCode.newBuilder().setExitCode(exitCode).build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Failed to properly wait for container exit: {}", containerId, e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            } finally {
                ftepDockerService.removeContainer(dockerClient, containerId);
            }
        }
    }

    @Override
    public void waitForContainerExitWithTimeout(ExitWithTimeoutParams request, StreamObserver<ContainerExitCode> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            String jobId = request.getJob().getId();
            checkPreconditions(jobId);

            DockerClient dockerClient = jobClients.get(jobId);
            String containerId = jobContainers.get(jobId);
            try {
                LOG.info("Waiting {} minutes for application to exit (job {})", request.getTimeout(), jobId);
                int exitCode = waitForContainer(dockerClient, containerId).awaitStatusCode(request.getTimeout(), TimeUnit.MINUTES);
                responseObserver.onNext(ContainerExitCode.newBuilder().setExitCode(exitCode).build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                if (e.getClass().equals(DockerClientException.class) && e.getMessage().equals("Awaiting status code timeout.")) {
                    LOG.warn("Timed out waiting for application to exit; manually stopping container and treating as 'normal' exit: {}", containerId);
                    stopContainer(dockerClient, containerId);
                    responseObserver.onNext(ContainerExitCode.newBuilder().setExitCode(0).build());
                    responseObserver.onCompleted();
                } else {
                    LOG.error("Failed to properly wait for container exit: {}", containerId, e);
                    responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
                }
            } finally {
                ftepDockerService.removeContainer(dockerClient, containerId);
            }
        }
    }

    @Override
    public void listOutputFiles(ListOutputFilesParam request, StreamObserver<OutputFileList> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            Path outputDir = Paths.get(request.getOutputsRootPath());
            LOG.debug("Listing outputs from job {} in path: {}", request.getJob().getId(), outputDir);

            OutputFileList.Builder responseBuilder = OutputFileList.newBuilder();

            try (Stream<Path> outputDirContents = Files.walk(outputDir, 3, FileVisitOption.FOLLOW_LINKS)) {
                outputDirContents.filter(Files::isRegularFile)
                        .map(Unchecked.function(outputDir::relativize))
                        .peek(relativePath -> LOG.debug("Found output file for job {} with relative path: {}", request.getJob().getId(), relativePath))
                        .map(relativePath -> OutputFileItem.newBuilder().setRelativePath(relativePath.toString()).build())
                        .forEach(responseBuilder::addItems);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to list output files: {}", request.toString(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    @Override
    public void getOutputFile(GetOutputFileParam request, StreamObserver<FileStream> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            try (FileStreamServer fileStreamServer = new FileStreamServer(Paths.get(request.getPath()), responseObserver) {
                @Override
                protected FileStream.FileMeta buildFileMeta() {
                    try {
                        return FileStream.FileMeta.newBuilder()
                                .setFilename(getInputPath().getFileName().toString())
                                .setSize(Files.size(getInputPath()))
                                .build();
                    } catch (IOException e) {
                        throw new FileStreamIOException(e);
                    }
                }

                @Override
                protected ReadableByteChannel buildByteChannel() {
                    try {
                        return Files.newByteChannel(getInputPath(), StandardOpenOption.READ);
                    } catch (IOException e) {
                        throw new FileStreamIOException(e);
                    }
                }
            }) {
                LOG.info("Returning output file from job {}: {} ({} bytes)", request.getJob().getId(), fileStreamServer.getInputPath(), fileStreamServer.getFileMeta().getSize());
                Stopwatch stopwatch = Stopwatch.createStarted();
                fileStreamServer.streamFile();
                LOG.info("Transferred output file {} ({} bytes) in {}", fileStreamServer.getInputPath().getFileName(), fileStreamServer.getFileMeta().getSize(), stopwatch.stop().elapsed());
            } catch (IOException e) {
                LOG.error("Failed to collect output file: {}", request.toString(), e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            } catch (InterruptedException e) {
                // Restore interrupted state
                Thread.currentThread().interrupt();
                LOG.error("Failed to collect output file: {}", request.toString(), e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    @Override
    public void prepareDockerImage(DockerImageConfig dockerImageConfig, StreamObserver<PrepareDockerImageResponse> responseObserver) {
        // TODO provision node for docker building?
        DockerClient dockerClient = ftepDockerService.getDockerClient(DOCKER_HOST_URL, dockerRegistryConfig);
        prepareDockerImage(dockerClient, dockerImageConfig, responseObserver);
    }

    @Override
    public void getJobEnvironment(GetJobEnvironmentRequest request, StreamObserver<JobEnvironment> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            try {
                com.cgi.eoss.ftep.worker.worker.JobEnvironment jobEnvironment = jobEnvironmentService.getExistingJobEnvironment(request.getJob().getId());
                responseObserver.onNext(JobEnvironment.newBuilder()
                        .setInputDir(jobEnvironment.getInputDir().toAbsolutePath().toString())
                        .setOutputDir(jobEnvironment.getOutputDir().toAbsolutePath().toString())
                        .setWorkingDir(jobEnvironment.getWorkingDir().toAbsolutePath().toString())
                        .setTempDir(jobEnvironment.getTempDir().toAbsolutePath().toString())
                        .build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(Status.Code.ABORTED).withCause(e)));
                LOG.error("Failed to retrieve an existing job environment for job ID {}", request.getJob().getId());
            }
        }
    }

    @Override
    public void getResumableJobs(GetResumableJobsRequest request, StreamObserver<GetResumableJobsResponse> responseObserver) {
        try {
            Node node = nodes.get(request.getNodeId());
            List<JobContainer> jobs = getResumableJobs(request.getWorkerId(), node);
            responseObserver.onNext(GetResumableJobsResponse.newBuilder().addAllJobs(jobs).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(Status.Code.ABORTED).withCause(e)));
            LOG.error("Failed to retrieve resumable jobs for worker {}", request.getWorkerId());
        }
    }

    @Override
    public void cleanUp(com.cgi.eoss.ftep.rpc.Job job, io.grpc.stub.StreamObserver<com.cgi.eoss.ftep.rpc.worker.CleanUpResponse> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(job)) {
            String jobId = job.getId();
            try {
                nodeManager.releaseStorageForJob(jobId);
            } catch (StorageProvisioningException e) {
                LOG.error("Exception releasing storage ", e);
            }
            externalInputs.removeAll(jobId);
            LOG.info("Clean up requested for job {}", jobId);
            jobContainers.remove(jobId);
            jobClients.remove(jobId);
            Optional.ofNullable(jobEnvironments.remove(jobId)).ifPresent(je -> {
                if (!keepProcDir) {
                    LOG.info("Clean up environment for: {}, keepProcDir {}", je.getJobId(), keepProcDir);
                    destroyEnvironment(je);
                }
            });
            jobNodes.remove(jobId);
            nodeManager.releaseJobNode(jobId);
            Set<URI> finishedJobInputs = ImmutableSet.copyOf(jobInputs.removeAll(jobId));
            LOG.debug("Finished job using URIs: {}", finishedJobInputs);
            LOG.debug("Finding the difference between {} and {}", finishedJobInputs, jobInputs.values());
            Set<URI> unusedUris = Sets.difference(finishedJobInputs, ImmutableSet.copyOf(jobInputs.values()));
            LOG.debug("Unused URIs to be cleaned: {}", unusedUris);
            inputOutputManager.cleanUp(unusedUris);
        } finally {
            responseObserver.onNext(CleanUpResponse.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void terminateJob(TerminateJobRequest request, StreamObserver<ContainerExitCode> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            String jobId = request.getJob().getId();
            DockerClient dockerClient = jobClients.get(jobId);
            String containerId = jobContainers.get(jobId);
            try {
                LOG.warn("Job {} has timed out. Manually stopping the container {} and treating as 'normal' exit", jobId, containerId);
                stopContainer(dockerClient, containerId);
                responseObserver.onNext(ContainerExitCode.newBuilder().setExitCode(0).build());
                responseObserver.onCompleted();
            } finally {
                ftepDockerService.removeContainer(dockerClient, containerId);
            }
        }
    }

    private Node prepareNode(String jobId) {
        Node node = nodeManager.getJobNode(jobId);
        LOG.debug("Tracking job {} on node {}", jobId, node);
        jobNodes.putIfAbsent(jobId, node);
        return node;
    }

    private void prepareDockerClient(String jobId, Node node) {
        LOG.info("Finding Docker client for {}", jobId);
        DockerClient dockerClient = ftepDockerService.getDockerClient(node.getDockerEngineUrl(), dockerRegistryConfig);
        LOG.debug("Tracking job {} on Docker client {}", jobId, dockerClient);
        jobClients.putIfAbsent(jobId, dockerClient);
    }

    private JobEnvironment prepareJobEnvironment(JobInputs request, String jobId) throws IOException {
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(request.getInputsList());

        // Create workspace directories and input parameters file
        com.cgi.eoss.ftep.worker.worker.JobEnvironment jobEnv = jobEnvironmentService.createEnvironment(jobId, inputs);

        // Resolve and download any URI-type inputs
        for (Map.Entry<String, String> e : inputs.entries()) {
            if (isValidUri(e.getValue())) {
                Path subdirPath = jobEnv.getInputDir().resolve(e.getKey());

                // Just hope no one has used a comma in their url...
                Set<URI> inputUris = Arrays.stream(StringUtils.split(e.getValue(), ',')).map(URI::create).collect(toSet());
                inputOutputManager.prepareInput(subdirPath, inputUris).values().forEach(value -> externalInputs.get(jobId).add(value));
                jobInputs.putAll(jobId, inputUris);
            }
        }

        jobEnvironments.putIfAbsent(jobId, jobEnv);

        return JobEnvironment.newBuilder()
                .setInputDir(jobEnv.getInputDir().toAbsolutePath().toString())
                .setOutputDir(jobEnv.getOutputDir().toAbsolutePath().toString())
                .setWorkingDir(jobEnv.getWorkingDir().toAbsolutePath().toString())
                .setTempDir(jobEnv.getTempDir().toAbsolutePath().toString())
                .build();
    }

    private String createContainer(JobSpec jobSpec, DockerClient dockerClient) throws IOException, StorageProvisioningException {
        String jobId = jobSpec.getJob().getId();
        String imageTag;
        String dockerImageTag = jobSpec.getService().getDockerImageTag();

        // If available, pull the Docker image from the registry
        if (Optional.ofNullable(dockerRegistryConfig).isPresent()) {
            String registryImageTag = dockerRegistryConfig.getDockerRegistryUrl() + "/" + dockerImageTag;
            try {
                ftepDockerService.pullDockerImage(dockerClient, registryImageTag, dockerRegistryConfig);
                // Use registry image tag
                imageTag = registryImageTag;
            } catch (Exception e) {
                LOG.info("Failed to pull image {} from registry '{}'", registryImageTag, dockerRegistryConfig.getDockerRegistryUrl());
                // Use Docker image tag
                imageTag = dockerImageTag;
            }
        } else {
            // Use Docker image tag
            imageTag = dockerImageTag;
        }

        // If registry is not available or if pulling the image from the registry failed, build the image
        if (!ftepDockerService.isImageAvailableLocally(dockerClient, imageTag)) {
            LOG.info("Building image '{}' locally", imageTag);
            buildDockerImage(dockerClient, jobSpec.getService().getName(), imageTag, null);
        }

        // Launch tag
        return ftepDockerService.createContainer(dockerClient, jobSpec, imageTag, prepareBindsForDockerContainer(jobSpec)).getId();
    }

    private PortBindings getBindings(DockerClient dockerClient, String containerId) {
        LOG.debug("Inspecting container for port bindings: {}", containerId);
        InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();
        Map<ExposedPort, Ports.Binding[]> exposedPortMap = inspectContainerResponse.getNetworkSettings().getPorts().getBindings();

        LOG.debug("Returning port map: {}", exposedPortMap);
        PortBindings.Builder bindingsBuilder = PortBindings.newBuilder();
        exposedPortMap.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> PortBinding.newBuilder()
                        .setPortDef(e.getKey().toString())
                        .setBinding(Binding.newBuilder().setIp(e.getValue()[0].getHostIp()).setPort(Integer.parseInt(e.getValue()[0].getHostPortSpec())).build())
                        .build())
                .forEach(bindingsBuilder::addBindings);
        return bindingsBuilder.build();
    }

    private List<String> prepareBindsForDockerContainer(JobSpec jobSpec) throws StorageProvisioningException {
        com.cgi.eoss.ftep.worker.worker.JobEnvironment jobEnvironment = jobEnvironments.get(jobSpec.getJob().getId());

        // TODO Take mount paths from config
        Set<String> binds = externalInputsToDockerDataBinds(externalInputs.get(jobSpec.getJob().getId()));
        binds.add(jobEnvironment.getWorkingDir().toAbsolutePath() + "/FTEP-WPS-INPUT.properties:/home/worker/workDir/FTEP-WPS-INPUT.properties:ro");
        binds.add(jobEnvironment.getInputDir().toAbsolutePath() + ":" + "/home/worker/workDir/inDir:ro");
        binds.add(jobEnvironment.getOutputDir().toAbsolutePath() + ":" + "/home/worker/workDir/outDir:rw");

        if (jobSpec.hasResourceRequest()) {
            ResourceRequest resourceRequest = jobSpec.getResourceRequest();
            int requiredStorage = resourceRequest.getStorage();
            String procDir = "proc-" + UUID.randomUUID().toString();
            String storageTempDir = Paths.get("/dockerStorage", procDir).toString();
            nodeManager.allocateStorageForJob(jobSpec.getJob().getId(), requiredStorage, storageTempDir);
            binds.add(storageTempDir + ":" + "/home/worker/procDir:rw");
        } else {
            binds.add(jobEnvironment.getTempDir().toAbsolutePath() + ":" + "/home/worker/procDir:rw");
        }

        // TODO Implement user-specific volumes feature
        binds.addAll(jobSpec.getUserBindsList());

        LOG.debug("Docker binds: {}", binds);
        return new ArrayList<>(binds);
    }

    private Set<String> externalInputsToDockerDataBinds(Set<Path> jobExternalInputs) {
        return jobExternalInputs.stream().map(p -> {
            try {
                return String.format("%s:%s:ro", p.toRealPath().toAbsolutePath().toString(), p.toRealPath().toAbsolutePath().toString());
            } catch (Exception e) {
                LOG.debug("Failed to convert path toRealPath: {}: {}", p, e.getMessage());
                throw new RuntimeException(e);
            }
        }).collect(toSet());
    }

    private void destroyEnvironment(com.cgi.eoss.ftep.worker.worker.JobEnvironment jobEnvironment) {
        try {
            MoreFiles.deleteRecursively(jobEnvironment.getTempDir());
        } catch (IOException e) {
            LOG.warn("Failed to clean up job environment tempDir: {}: {}", jobEnvironment.getTempDir(), e.getMessage());
            LOG.trace("Exception cleaning up job environment {}", jobEnvironment.getTempDir(), e);
        }
    }

    private WaitContainerResultCallback waitForContainer(DockerClient dockerClient, String containerId) {
        return dockerClient.waitContainerCmd(containerId).exec(new WaitContainerResultCallback());
    }

    private void stopContainer(DockerClient client, String containerId) {
        if (client.inspectContainerCmd(containerId).exec().getState().getRunning()) {
            try {
                client.stopContainerCmd(containerId).withTimeout(30).exec();
                if (client.inspectContainerCmd(containerId).exec().getState().getRunning()) {
                    LOG.warn("Reached timeout trying to stop container safely; killing: {}", containerId);
                    client.killContainerCmd(containerId).exec();
                }
            } catch (DockerClientException e) {
                LOG.warn("Received exception trying to stop container; killing: {}", containerId, e);
                client.killContainerCmd(containerId).exec();
            }
        } else {
            LOG.debug("Container {} appears to already be stopped", containerId);
        }
    }

    private boolean isValidUri(String test) {
        try {
            URI uri = URI.create(test);
            return uri.getScheme() != null && inputOutputManager.isSupportedProtocol(uri.getScheme());
        } catch (Exception unused) {
            return false;
        }
    }

    private void prepareDockerImage(DockerClient dockerClient, DockerImageConfig dockerImageConfig, StreamObserver<PrepareDockerImageResponse> responseObserver) {
        String dockerImageTag = dockerImageConfig.getDockerImage();
        Lock lock = dockerBuildLock.get(dockerImageTag); // Avoid multiple parallel jobs trying to build at exactly the same time
        lock.lock();
        try {
            // Build an image with an appropriate tag, depending on the presence of the registry
            Optional<String> registryImageTag = Optional.ofNullable(dockerRegistryConfig)
                    .map(registryConfig -> registryConfig.getDockerRegistryUrl() + "/" + dockerImageTag);
            String imageTag = registryImageTag.orElse(dockerImageTag);

            // Pull the Docker image from the registry if applicable to warm up the cache
            if (Optional.ofNullable(dockerRegistryConfig).isPresent()) {
                try {
                    ftepDockerService.pullDockerImage(dockerClient, imageTag, dockerRegistryConfig);
                } catch (Exception e) {
                    LOG.debug("Failed to pull image {} from registry '{}'", imageTag, dockerRegistryConfig.getDockerRegistryUrl());
                }
            }

            buildDockerImage(dockerClient, dockerImageConfig.getServiceName(), imageTag, dockerImageConfig.getBuildFingerprint());

            // Push the built image into the registry if applicable
            if (Optional.ofNullable(dockerRegistryConfig).isPresent()) {
                try {
                    ftepDockerService.pushDockerImage(dockerClient, imageTag, dockerRegistryConfig);
                } catch (Exception e) {
                    LOG.warn("Failed to push image {}", imageTag, e);
                    throw e;
                }
            }

            responseObserver.onNext(PrepareDockerImageResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(Status.Code.ABORTED).withCause(e)));
            LOG.error("Failed preparing Docker image for service {}", dockerImageConfig.getServiceName(), e);
        } finally {
            lock.unlock();
        }
    }

    private void buildDockerImage(DockerClient dockerClient, String serviceName, String dockerImageTag, String fingerprint) throws IOException {
        Lock lock = dockerBuildLock.get(dockerImageTag); // Avoid multiple parallel jobs trying to build at exactly the same time
        lock.lock();
        try {
            try {
                // Retrieve service context files
                Path serviceContext = inputOutputManager.getServiceContext(serviceName);

                if (serviceContext == null) {
                    // If no service context files are available, shortcut and fall back on the hopefully-existent image tag
                    LOG.warn("No service context found for service '{}'; falling back on image tag", serviceName);
                    return;
                }
                try (Stream<Path> stream = Files.list(serviceContext)) {
                    if (stream.count() == 0) {
                        LOG.warn("No service context files found for service '{}'; falling back on image tag", serviceName);
                        return;
                    }
                }

                if (!Files.exists(serviceContext.resolve("Dockerfile"))) {
                    LOG.warn("Service context files exist, but no Dockerfile found for service '{}'; falling back on image tag", serviceName);
                    return;
                }

                // Build image
                String imageId = ftepDockerService.buildImage(dockerClient, serviceName, dockerImageTag, fingerprint, serviceContext);

                // Tag image with desired image name
                dockerClient.tagImageCmd(imageId, dockerImageTag, "").exec();
                LOG.debug("Tagged docker image {} with tag '{}'", imageId, dockerImageTag);
            } catch (ServiceIoException e) {
                Logging.withUserLoggingContext(() -> LOG.error("Failed to retrieve Docker context files for service {}", serviceName));
                LOG.error("Failed to retrieve Docker context files for service {}", serviceName, e);
                throw e;
            } catch (IOException e) {
                Logging.withUserLoggingContext(() -> LOG.error("Failed to build Docker context for service {}: {}", serviceName, e.getMessage()));
                LOG.error("Failed to build Docker context for service {}", serviceName, e);
                throw e;
            }
        } finally {
            lock.unlock();
        }
    }

    @VisibleForTesting
    Map<String, DockerClient> getJobClients() {
        return jobClients;
    }

    private List<JobContainer> getResumableJobs(String workerId, Node node) {

        // Set up a new connection to the Docker engine
        DockerClient dockerClient = ftepDockerService.getDockerClient(node.getDockerEngineUrl(), dockerRegistryConfig);

        // List all running/exited containers
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();

        // Return all service containers launched by this worker as JobContainer objects
        return containers.stream()
                .filter(container -> isMatchingContainer(container, workerId))
                .map(container -> trackJobData(container, dockerClient, node))
                .map(container -> convertToJobContainer(container, dockerClient))
                .collect(Collectors.toList());
    }

    /**
     * Return true if the container was used for running a service image and was launched by the given worker
     *
     * @param container
     * @param workerId
     * @return
     */
    private boolean isMatchingContainer(Container container, String workerId) {
        Map<String, String> labels = container.getLabels();
        return labels.containsKey(SERVICE_ID)
                && labels.containsKey(WORKER_ID)
                && labels.get(WORKER_ID).equals(workerId);
    }

    /**
     * Update maps to track data for the job
     *
     * @param container
     * @param dockerClient
     * @param node
     * @return
     */
    private Container trackJobData(Container container, DockerClient dockerClient, Node node) {
        String jobId = container.getLabels().get(JOB_ID);
        nodeManager.reattachJobToNode(node, jobId);
        jobContainers.putIfAbsent(jobId, container.getId());
        jobClients.putIfAbsent(jobId, dockerClient);
        jobNodes.putIfAbsent(jobId, node);
        return container;
    }

    private JobContainer convertToJobContainer(Container container, DockerClient dockerClient) {
        Map<String, String> labels = container.getLabels();
        Job job = Job.newBuilder()
                .setId(labels.get(JOB_ID))
                .setIntJobId(Long.valueOf(labels.get(INT_JOB_ID)))
                .setUserId(labels.get(USER_ID))
                .setServiceId(labels.get(SERVICE_ID))
                .build();
        return JobContainer.newBuilder().setJob(job).setContainerStatus(getContainerStatus(container, dockerClient)).build();
    }

    private ContainerStatus getContainerStatus(Container container, DockerClient dockerClient) {
        Map<String, String> labels = container.getLabels();

        // If timeout was used for launching this container, check if that time has already passed, regardless of the container status
        if (labels.containsKey(TIMEOUT_VALUE)) {
            int timeoutValue = Integer.parseInt(labels.get(TIMEOUT_VALUE));
            Optional<Instant> startTime = ftepDockerService.getContainerStarttime(container, dockerClient);
            if (startTime.isPresent() && Instant.now().isAfter(startTime.get().plus(timeoutValue, ChronoUnit.MINUTES))) {
                return ContainerStatus.TIMEOUT;
            }
        }

        String containerStatus = dockerClient.inspectContainerCmd(container.getId()).exec().getState().getStatus();
        if (Arrays.asList("created", "restarting", "running", "removing", "paused").contains(containerStatus)) {
            return ContainerStatus.RUNNING;
        } else { // "dead", "exited"
            return ContainerStatus.COMPLETED;
        }
    }

    @Override
    public void getNodes(GetNodesRequest request, StreamObserver<GetNodesResponse> responseObserver) {
        try {
            Set<Node> currentNodes = nodeManager.getCurrentNodes();
            nodes.putAll(currentNodes.stream().collect(toMap(Node::getId, node -> node)));
            responseObserver.onNext(GetNodesResponse.newBuilder()
                    .addAllNodes(currentNodes.stream().map(this::nodeToProtobuf).collect(Collectors.toList()))
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(Status.Code.ABORTED).withCause(e)));
            LOG.error("Failed to get current nodes");
        }
    }

    private com.cgi.eoss.ftep.rpc.worker.Node nodeToProtobuf(Node node) {
        return com.cgi.eoss.ftep.rpc.worker.Node.newBuilder()
                .setId(node.getId())
                .build();
    }

    private void checkPreconditions(String jobId) {
        Preconditions.checkArgument(jobClients.containsKey(jobId), "Job ID %s is not attached to a DockerClient", jobId);
        Preconditions.checkArgument(jobContainers.containsKey(jobId), "Job ID %s does not have a known container ID", jobId);
    }
}
