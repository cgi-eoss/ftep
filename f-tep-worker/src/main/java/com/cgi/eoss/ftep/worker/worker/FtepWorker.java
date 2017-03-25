package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
import com.cgi.eoss.ftep.rpc.worker.LaunchContainerResponse;
import com.cgi.eoss.ftep.rpc.worker.OutputFileParam;
import com.cgi.eoss.ftep.rpc.worker.OutputFileResponse;
import com.cgi.eoss.ftep.worker.docker.DockerClientFactory;
import com.cgi.eoss.ftep.worker.docker.Log4jContainerCallback;
import com.cgi.eoss.ftep.worker.io.ServiceInputOutputManager;
import com.cgi.eoss.ftep.worker.io.ServiceIoException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>Service for executing F-TEP (WPS) services inside Docker containers.</p>
 */
@GRpcService
@Log4j2
public class FtepWorker extends FtepWorkerGrpc.FtepWorkerImplBase {

    private static final int FILE_STREAM_CHUNK_BYTES = 8192;

    private final DockerClientFactory dockerClientFactory;
    private final JobEnvironmentService jobEnvironmentService;
    private final ServiceInputOutputManager inputOutputManager;

    // Track which DockerClient is used for each job
    private final Map<String, DockerClient> jobClients = new HashMap<>();
    // Track which container ID is used for each job
    private final Map<String, String> jobContainers = new HashMap<>();

    @Autowired
    public FtepWorker(DockerClientFactory dockerClientFactory, JobEnvironmentService jobEnvironmentService, ServiceInputOutputManager inputOutputManager) {
        this.dockerClientFactory = dockerClientFactory;
        this.jobEnvironmentService = jobEnvironmentService;
        this.inputOutputManager = inputOutputManager;
    }

    @Override
    public void prepareEnvironment(JobInputs request, StreamObserver<JobEnvironment> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            try {
                DockerClient dockerClient = dockerClientFactory.getDockerClient();
                jobClients.put(request.getJob().getId(), dockerClient);
            } catch (Exception e) {
                LOG.error("Failed to prepare Docker context for {}", request.getJob().getId(), e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }

            try {
                Multimap<String, String> inputs = GrpcUtil.paramsListToMap(request.getInputsList());

                // Create workspace directories and input parameters file
                com.cgi.eoss.ftep.worker.worker.JobEnvironment jobEnv = jobEnvironmentService.createEnvironment(request.getJob().getId(), inputs);

                // Resolve and download any URI-type inputs
                for (Map.Entry<String, String> e : inputs.entries()) {
                    if (isValidUri(e.getValue())) {
                        Path subdirPath = jobEnv.getInputDir().resolve(e.getKey());
                        inputOutputManager.prepareInput(subdirPath, URI.create(e.getValue()));
                    }
                }

                JobEnvironment ret = JobEnvironment.newBuilder()
                        .setInputDir(jobEnv.getInputDir().toAbsolutePath().toString())
                        .setOutputDir(jobEnv.getOutputDir().toAbsolutePath().toString())
                        .setWorkingDir(jobEnv.getWorkingDir().toAbsolutePath().toString())
                        .build();

                responseObserver.onNext(ret);
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Failed to prepare job inputs for {}", request.getJob().getId(), e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    @Override
    public void launchContainer(JobDockerConfig request, StreamObserver<LaunchContainerResponse> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            Preconditions.checkArgument(jobClients.containsKey(request.getJob().getId()), "Job ID {} is not attached to a DockerClient", request.getJob().getId());

            DockerClient dockerClient = jobClients.get(request.getJob().getId());
            String containerId = null;

            try {
                buildDockerImage(dockerClient, request.getServiceName(), request.getDockerImage());

                // Launch tag
                CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(request.getDockerImage());
                createContainerCmd.withBinds(request.getBindsList().stream().map(Bind::parse).collect(Collectors.toList()));
                createContainerCmd.withExposedPorts(request.getPortsList().stream().map(ExposedPort::parse).collect(Collectors.toList()));

                containerId = createContainerCmd.exec().getId();
                jobContainers.put(request.getJob().getId(), containerId);

                LOG.info("Launching container {} for job {}", containerId, request.getJob().getId());
                dockerClient.startContainerCmd(containerId).exec();

                dockerClient.logContainerCmd(containerId).withStdErr(true).withStdOut(true).withFollowStream(true).withTailAll()
                        .exec(new Log4jContainerCallback());

                responseObserver.onNext(LaunchContainerResponse.newBuilder().build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Failed to launch docker container {}", request.getDockerImage(), e);
                if (!Strings.isNullOrEmpty(containerId)) {
                    removeContainer(dockerClient, containerId);
                    jobContainers.remove(request.getJob().getId());
                    jobClients.remove(request.getJob().getId());
                }
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    @Override
    public void waitForContainerExit(ExitParams request, StreamObserver<ContainerExitCode> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            Preconditions.checkArgument(jobClients.containsKey(request.getJob().getId()), "Job ID {} is not attached to a DockerClient", request.getJob().getId());
            Preconditions.checkArgument(jobContainers.containsKey(request.getJob().getId()), "Job ID {} does not have a known container ID", request.getJob().getId());

            DockerClient dockerClient = jobClients.get(request.getJob().getId());
            String containerId = jobContainers.get(request.getJob().getId());
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
                jobContainers.remove(request.getJob().getId());
                jobClients.remove(request.getJob().getId());
            }
        }
    }

    @Override
    public void waitForContainerExitWithTimeout(ExitWithTimeoutParams request, StreamObserver<ContainerExitCode> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            Preconditions.checkArgument(jobClients.containsKey(request.getJob().getId()), "Job ID %s is not attached to a DockerClient", request.getJob().getId());
            Preconditions.checkArgument(jobContainers.containsKey(request.getJob().getId()), "Job ID %s does not have a known container ID", request.getJob().getId());

            DockerClient dockerClient = jobClients.get(request.getJob().getId());
            String containerId = jobContainers.get(request.getJob().getId());
            try {
                int exitCode = waitForContainer(dockerClient, containerId).awaitStatusCode(request.getTimeout(), TimeUnit.HOURS);
                responseObserver.onNext(ContainerExitCode.newBuilder().setExitCode(exitCode).build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Failed to properly wait for container exit: {}", containerId, e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            } finally {
                removeContainer(dockerClient, containerId);
                jobContainers.remove(request.getJob().getId());
                jobClients.remove(request.getJob().getId());
            }
        }
    }

    @Override
    public void getOutputFile(OutputFileParam request, StreamObserver<OutputFileResponse> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            String outputId = request.getOutputId();
            try {
                Path outputDir = Paths.get(request.getPath()).resolve(outputId);
                if (Files.list(outputDir).count() != 1) {
                    throw new ServiceIoException("Found " + Files.list(outputDir).count() + " files in the output directory (1 expected): " + outputDir);
                }
                Path outputFile = Files.list(outputDir).findFirst().orElseThrow(FileNotFoundException::new);
                long outputFileSize = Files.size(outputFile);
                LOG.info("Returning output file from job {}: {} ({} bytes)", request.getJob().getId(), outputFile, outputFileSize);

                // First message is the metadata
                OutputFileResponse.FileMeta fileMeta = OutputFileResponse.FileMeta.newBuilder()
                        .setFilename(outputFile.getFileName().toString())
                        .setSize(outputFileSize)
                        .build();
                responseObserver.onNext(OutputFileResponse.newBuilder().setMeta(fileMeta).build());

                // Then read the file, chunked at 8kB
                LocalDateTime startTime = LocalDateTime.now();
                try (SeekableByteChannel channel = Files.newByteChannel(outputFile, StandardOpenOption.READ)) {
                    ByteBuffer buffer = ByteBuffer.allocate(FILE_STREAM_CHUNK_BYTES);
                    int position = 0;
                    while (channel.read(buffer) > 0) {
                        int size = buffer.position();
                        buffer.rewind();
                        responseObserver.onNext(OutputFileResponse.newBuilder().setChunk(OutputFileResponse.Chunk.newBuilder()
                                .setPosition(position)
                                .setData(ByteString.copyFrom(buffer, size))
                                .build()).build());
                        position += buffer.position();
                        buffer.flip();
                    }
                }
                LOG.info("Transferred output file {} ({} bytes) in {}", outputFile.getFileName(), outputFileSize, Duration.between(startTime, LocalDateTime.now()));

                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Failed to collect output file: {}", request.toString(), e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    private WaitContainerResultCallback waitForContainer(DockerClient dockerClient, String containerId) {
        return dockerClient.waitContainerCmd(containerId).exec(new WaitContainerResultCallback());
    }

    private void removeContainer(DockerClient client, String containerId) {
        try {
            LOG.info("Removing container {}", containerId);
            client.removeContainerCmd(containerId).exec();
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
            LOG.info("Building Docker image '{}' for service {}", dockerImage, serviceName);
            String imageId = dockerClient.buildImageCmd()
                    .withBaseDirectory(serviceContext.toFile())
                    .withDockerfile(serviceContext.resolve("Dockerfile").toFile())
                    .withTag(dockerImage)
                    .exec(new BuildImageResultCallback()).awaitImageId();

            // Tag image with desired image name
            LOG.debug("Tagged docker image {} with tag '{}'", imageId, dockerImage);
        } catch (ServiceIoException e) {
            LOG.error("Failed to retrieve Docker context files for service {}", serviceName, e);
            throw e;
        } catch (IOException e) {
            LOG.error("Failed to build Docker context for service {}", serviceName, e);
            throw e;
        }
    }

    @VisibleForTesting
    Map<String, DockerClient> getJobClients() {
        return jobClients;
    }

    @VisibleForTesting
    Map<String, String> getJobContainers() {
        return jobContainers;
    }

    private static CloseableThreadContext.Instance getJobLoggingContext(Job job) {
        return CloseableThreadContext.push("F-TEP Worker")
                .put("zooId", job.getId())
                .put("jobId", job.getIntJobId())
                .put("userId", job.getUserId())
                .put("serviceId", job.getServiceId());
    }

}
