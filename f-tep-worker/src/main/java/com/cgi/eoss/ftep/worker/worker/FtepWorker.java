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
import com.cgi.eoss.ftep.rpc.NoopStreamObserver;
import com.cgi.eoss.ftep.rpc.worker.Binding;
import com.cgi.eoss.ftep.rpc.worker.CleanUpResponse;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.DockerImageConfig;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.GetOutputFileParam;
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
import com.cgi.eoss.ftep.worker.DockerRegistryConfig;
import com.cgi.eoss.ftep.worker.docker.DockerClientFactory;
import com.cgi.eoss.ftep.worker.docker.Log4jContainerCallback;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.exception.BadRequestException;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
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

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final boolean keepProcDir;

    private DockerRegistryConfig dockerRegistryConfig;

    // Track which Node is used for each job
    private final Map<String, Node> jobNodes = new HashMap<>();
    // Track which JobEnvironment is used for each job
    private final Map<String, com.cgi.eoss.ftep.worker.worker.JobEnvironment> jobEnvironments = new HashMap<>();
    // Track which DockerClient is used for each job
    private final Map<String, DockerClient> jobClients = new HashMap<>();
    // Track which container ID is used for each job
    private final Map<String, String> jobContainers = new HashMap<>();
    // Track which input URIs are used for each job
    private final SetMultimap<String, URI> jobInputs = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    private final Striped<Lock> dockerBuildLock = Striped.lazyWeakLock(1);

    private final int minWorkerNodes;

    private final SetMultimap<String, Path> externalInputs = HashMultimap.create();

    private final static String DOCKER_HOST_URL = "unix:///var/run/docker.sock";

    @Autowired
    public FtepWorker(FtepWorkerNodeManager nodeManager, JobEnvironmentService jobEnvironmentService,
                      ServiceInputOutputManager inputOutputManager, @Qualifier("minWorkerNodes") int minWorkerNodes,
                      @Qualifier("keepProcDir") boolean keepProcDir) {
        this.nodeManager = nodeManager;
        this.jobEnvironmentService = jobEnvironmentService;
        this.inputOutputManager = inputOutputManager;
        this.minWorkerNodes = minWorkerNodes;
        this.keepProcDir = keepProcDir;
    }

    @Autowired(required = false)
    public void setDockerRegistryConfig(DockerRegistryConfig dockerRegistryConfig) {
        this.dockerRegistryConfig = dockerRegistryConfig;
    }

    @PostConstruct
    public void allocateMinNodes() {
        int currentNodes = nodeManager.getCurrentNodes(FtepWorkerNodeManager.POOLED_WORKER_TAG).size();
        if (currentNodes < minWorkerNodes) {
            try {
                nodeManager.provisionNodes(minWorkerNodes - currentNodes, FtepWorkerNodeManager.POOLED_WORKER_TAG, jobEnvironmentService.getBaseDir());
            } catch (NodeProvisioningException e) {
                LOG.error("Failed initial node provisioning: {}", e.getMessage());
            }
        }
    }

    private static CloseableThreadContext.Instance getJobLoggingContext(Job job) {
        return CloseableThreadContext.push("F-TEP Worker")
                .put("zooId", job.getId())
                .put("jobId", String.valueOf(job.getIntJobId()))
                .put("userId", job.getUserId())
                .put("serviceId", job.getServiceId());
    }

    @Override
    public void prepareInputs(JobInputs request, StreamObserver<JobEnvironment> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            String jobId = request.getJob().getId();
            Node node;
            DockerClient dockerClient;
            try {
                LOG.info("Finding node for {}", jobId);
                node = nodeManager.findJobNode(jobId).orElseGet(() -> {
                    LOG.warn("Provisioning new node for {}, this should already have happened!", jobId);
                    nodeManager.reserveNodeForJob(jobId);
                    return nodeManager.provisionNodeForJob(jobEnvironmentService.getBaseDir(), jobId);
                });
            } catch (NodeProvisioningException e) {
                LOG.error("Failed to prepare Node for {}", jobId, e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
                return;
            }
            try {
                LOG.info("Finding Docker client for {}", jobId);
                if (dockerRegistryConfig != null) {
                    dockerClient = DockerClientFactory.buildDockerClient(node.getDockerEngineUrl(), dockerRegistryConfig);
                } else {
                    dockerClient = DockerClientFactory.buildDockerClient(node.getDockerEngineUrl());
                }
            } catch (Exception e) {
                Logging.withUserLoggingContext(() -> LOG.error("Failed to prepare Docker context: {}", e.getMessage()));
                LOG.error("Failed to prepare Docker context for {}", jobId, e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
                return;
            }

            LOG.debug("Tracking job {} on node {}", jobId, node);
            jobNodes.putIfAbsent(jobId, node);
            LOG.debug("Tracking job {} on Docker client {}", jobId, dockerClient);
            jobClients.putIfAbsent(jobId, dockerClient);

            try {
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

                JobEnvironment ret = JobEnvironment.newBuilder()
                        .setInputDir(jobEnv.getInputDir().toAbsolutePath().toString())
                        .setOutputDir(jobEnv.getOutputDir().toAbsolutePath().toString())
                        .setWorkingDir(jobEnv.getWorkingDir().toAbsolutePath().toString())
                        .setTempDir(jobEnv.getTempDir().toAbsolutePath().toString())
                        .build();
                jobEnvironments.putIfAbsent(jobId, jobEnv);

                responseObserver.onNext(ret);
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
                // TODO: remove call to prepareDockerImage once invoked from elsewhere
                Logging.withUserLoggingContext(() -> LOG.info("Preparing Docker image..."));
                prepareDockerImage(dockerClient, DockerImageConfig.newBuilder()
                        .setDockerImage(request.getService().getDockerImageTag())
                        .setServiceName(request.getService().getName())
                        .build(), new NoopStreamObserver<>());
                Logging.withUserLoggingContext(() -> LOG.info("Docker image prepared"));

                // Launch tag
                try (CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(request.getService().getDockerImageTag())) {
                    createContainerCmd.withLabels(ImmutableMap.of(
                            "jobId", jobId,
                            "intJobId", String.valueOf(request.getJob().getIntJobId()),
                            "userId", request.getJob().getUserId(),
                            "serviceId", request.getJob().getServiceId()
                    ));
                    createContainerCmd.withBinds(prepareBindsForDockerContainer(request).stream().map(Bind::parse).collect(Collectors.toList()));
                    createContainerCmd.withExposedPorts(request.getExposedPortsList().stream().map(ExposedPort::parse).collect(Collectors.toList()));
                    createContainerCmd.withPortBindings(request.getExposedPortsList().stream()
                            .map(p -> new com.github.dockerjava.api.model.PortBinding(new Ports.Binding(null, null), ExposedPort.parse(p)))
                            .collect(Collectors.toList()));

                    // Add proxy vars to the container, if they are set in the environment
                    createContainerCmd.withEnv(
                            ImmutableSet.of("http_proxy", "https_proxy", "no_proxy").stream()
                                    .filter(var -> System.getenv().containsKey(var))
                                    .map(var -> var + "=" + System.getenv(var))
                                    .collect(Collectors.toList()));

                    containerId = createContainerCmd.exec().getId();
                    jobContainers.put(jobId, containerId);
                }

                LOG.info("Launching container {} for job {}", containerId, jobId);
                dockerClient.startContainerCmd(containerId).exec();

                dockerClient.logContainerCmd(containerId).withStdErr(true).withStdOut(true).withFollowStream(true).withTailAll()
                        .exec(new Log4jContainerCallback());

                responseObserver.onNext(LaunchContainerResponse.newBuilder().build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                Logging.withUserLoggingContext(() -> LOG.error("Failed to launch Docker container: {}", e.getMessage()));
                LOG.error("Failed to launch Docker container {}", request.getService().getDockerImageTag(), e);
                if (!Strings.isNullOrEmpty(containerId)) {
                    removeContainer(dockerClient, containerId);
                }
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    @Override
    public void getPortBindings(Job request, StreamObserver<PortBindings> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request)) {
            Preconditions.checkArgument(jobClients.containsKey(request.getId()), "Job ID %s is not attached to a DockerClient", request.getId());
            Preconditions.checkArgument(jobContainers.containsKey(request.getId()), "Job ID %s does not have a known container ID", request.getId());

            DockerClient dockerClient = jobClients.get(request.getId());
            String containerId = jobContainers.get(request.getId());
            try {
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

                responseObserver.onNext(bindingsBuilder.build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                if (!Strings.isNullOrEmpty(containerId)) {
                    removeContainer(dockerClient, containerId);
                }
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    @Override
    public void stopContainer(Job request, StreamObserver<StopContainerResponse> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request)) {
            Preconditions.checkArgument(jobClients.containsKey(request.getId()), "Job ID %s is not attached to a DockerClient", request.getId());
            Preconditions.checkArgument(jobContainers.containsKey(request.getId()), "Job ID %s does not have a known container ID", request.getId());

            DockerClient dockerClient = jobClients.get(request.getId());
            String containerId = jobContainers.get(request.getId());

            LOG.info("Stop requested for job {} running in container {}", request.getId(), containerId);

            try {
                stopContainer(dockerClient, containerId);
                responseObserver.onNext(StopContainerResponse.newBuilder().build());
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
            Preconditions.checkArgument(jobClients.containsKey(jobId), "Job ID %s is not attached to a DockerClient", jobId);
            Preconditions.checkArgument(jobContainers.containsKey(jobId), "Job ID %s does not have a known container ID", jobId);

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
                removeContainer(dockerClient, containerId);
            }
        }
    }

    @Override
    public void waitForContainerExitWithTimeout(ExitWithTimeoutParams request, StreamObserver<ContainerExitCode> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            String jobId = request.getJob().getId();
            Preconditions.checkArgument(jobClients.containsKey(jobId), "Job ID %s is not attached to a DockerClient", jobId);
            Preconditions.checkArgument(jobContainers.containsKey(jobId), "Job ID %s does not have a known container ID", jobId);

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
                removeContainer(dockerClient, containerId);
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
            responseObserver.onNext(CleanUpResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
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

    private void removeContainer(DockerClient client, String containerId) {
        try {
            LOG.info("Removing container {}", containerId);
            client.removeContainerCmd(containerId).exec();
        } catch (BadRequestException e) {
            if (!e.getMessage().endsWith("is already in progress")) {
                LOG.error("Failed to delete container {}", containerId, e);
            }
        } catch (Exception e) {
            LOG.error("Failed to delete container {}", containerId, e);
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

    @Override
    public void prepareDockerImage(DockerImageConfig dockerImageConfig, StreamObserver<PrepareDockerImageResponse> responseObserver) {
        // TODO provision node for docker building?
        DockerClient dockerClient = Optional.ofNullable(dockerRegistryConfig)
                .map(config -> DockerClientFactory.buildDockerClient(DOCKER_HOST_URL, config))
                .orElseGet(() -> DockerClientFactory.buildDockerClient(DOCKER_HOST_URL));
        prepareDockerImage(dockerClient, dockerImageConfig, responseObserver);
    }

    private void prepareDockerImage(DockerClient dockerClient, DockerImageConfig dockerImageConfig, StreamObserver<PrepareDockerImageResponse> responseObserver) {
        String dockerImageTag = dockerImageConfig.getDockerImage();
        Lock lock = dockerBuildLock.get(dockerImageTag); // Avoid multiple parallel jobs trying to build at exactly the same time
        lock.lock();
        try {
            Optional<String> registryImageTag = Optional.ofNullable(dockerRegistryConfig)
                    .map(registryConfig -> registryConfig.getDockerRegistryUrl() + "/" + dockerImageTag);

            // Warm up the cache, if the registry is available
            if (registryImageTag.isPresent()) {
                try {
                    pullDockerImage(dockerClient, registryImageTag.get());
                } catch (Exception e) {
                    LOG.debug("Failed to pull image {}", registryImageTag.get());
                }
            }

            buildDockerImage(dockerClient, dockerImageConfig.getServiceName(), dockerImageTag);

            if (registryImageTag.isPresent()) {
                try {
                    dockerClient.tagImageCmd(dockerImageTag, registryImageTag.get(), "").exec();
                    pushDockerImage(dockerClient, registryImageTag.get());
                } catch (Exception e) {
                    LOG.warn("Failed to push image {}", registryImageTag.get(), e);
                }
            }

            responseObserver.onNext(PrepareDockerImageResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(Status.Code.ABORTED).withCause(e)));
            LOG.error("Failed preparing Docker image for service {}", dockerImageConfig.getServiceName(), e);
        } finally {
            lock.unlock();
        }
    }

    private void buildDockerImage(DockerClient dockerClient, String serviceName, String dockerImage) throws IOException {
        try {
            // Retrieve service context files
            Path serviceContext = inputOutputManager.getServiceContext(serviceName);

            if (serviceContext == null || Files.list(serviceContext).count() == 0) {
                // If no service context files are available, shortcut and fall back on the hopefully-existent image tag
                LOG.warn("No service context files found for service '{}'; falling back on image tag", serviceName);
                return;
            } else if (!Files.exists(serviceContext.resolve("Dockerfile"))) {
                LOG.warn("Service context files exist, but no Dockerfile found for service '{}'; falling back on image tag", serviceName);
                return;
            }

            // Build image
            LOG.info("Starting Docker image build '{}' for service {}", dockerImage, serviceName);
            BuildImageCmd buildImageCmd = dockerClient.buildImageCmd()
                    .withRemove(true)
                    .withBaseDirectory(serviceContext.toFile())
                    .withDockerfile(serviceContext.resolve("Dockerfile").toFile())
                    .withTags(ImmutableSet.of(dockerImage));

            // Add proxy vars to the container, if they are set in the environment
            ImmutableSet.of("http_proxy", "https_proxy", "no_proxy").stream()
                    .filter(var -> System.getenv().containsKey(var))
                    .forEach(var -> buildImageCmd.withBuildArg(var, System.getenv(var)));

            String imageId = buildImageCmd.exec(new BuildImageResultCallback()).awaitImageId();

            // Tag image with desired image name
            LOG.debug("Tagged docker image {} with tag '{}'", imageId, dockerImage);
        } catch (ServiceIoException e) {
            Logging.withUserLoggingContext(() -> LOG.error("Failed to retrieve Docker context files for service {}", serviceName));
            LOG.error("Failed to retrieve Docker context files for service {}", serviceName, e);
            throw e;
        } catch (IOException e) {
            Logging.withUserLoggingContext(() -> LOG.error("Failed to build Docker context for service {}: {}", serviceName, e.getMessage()));
            LOG.error("Failed to build Docker context for service {}", serviceName, e);
            throw e;
        }
    }

    private void pushDockerImage(DockerClient dockerClient, String dockerImage) throws IOException, InterruptedException {
        LOG.info("Pushing Docker image '{}' to registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
        PushImageCmd pushImageCmd = dockerClient.pushImageCmd(dockerImage);
        AuthConfig authConfig = new AuthConfig()
                .withRegistryAddress(dockerRegistryConfig.getDockerRegistryUrl())
                .withUsername(dockerRegistryConfig.getDockerRegistryUsername())
                .withPassword(dockerRegistryConfig.getDockerRegistryPassword());
        dockerClient.authCmd().withAuthConfig(authConfig).exec();
        pushImageCmd = pushImageCmd.withAuthConfig(authConfig);
        pushImageCmd.exec(new PushImageResultCallback()).awaitSuccess();
        LOG.info("Pushed Docker image '{}' to registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
    }

    private void pullDockerImage(DockerClient dockerClient, String dockerImage) throws IOException, InterruptedException {
        LOG.info("Pulling Docker image '{}' from registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(dockerImage);
        AuthConfig authConfig = new AuthConfig()
                .withRegistryAddress(dockerRegistryConfig.getDockerRegistryUrl())
                .withUsername(dockerRegistryConfig.getDockerRegistryUsername())
                .withPassword(dockerRegistryConfig.getDockerRegistryPassword());
        dockerClient.authCmd().withAuthConfig(authConfig).exec();
        pullImageCmd = pullImageCmd.withRegistry(dockerRegistryConfig.getDockerRegistryUrl()).withAuthConfig(authConfig);
        pullImageCmd.exec(new PullImageResultCallback()).awaitSuccess();
        LOG.info("Pulled Docker image '{}' from registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
    }

    private void removeDockerImage(DockerClient dockerClient, String dockerImageTag) {
        List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(dockerImageTag).exec();
        for (Image image : images) {
            dockerClient.removeImageCmd(image.getId()).exec();
        }
    }

    private boolean isImageAvailableLocally(DockerClient dockerClient, String dockerImage) {
        List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(dockerImage).exec();
        return !images.isEmpty();
    }

    @VisibleForTesting
    Map<String, DockerClient> getJobClients() {
        return jobClients;
    }

}
