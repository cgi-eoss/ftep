package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
import com.cgi.eoss.ftep.rpc.worker.LaunchContainerResponse;
import com.cgi.eoss.ftep.worker.docker.DockerClientFactory;
import com.cgi.eoss.ftep.worker.io.ServiceInputOutputManager;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.nio.file.Path;
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
        try {
            DockerClient dockerClient = dockerClientFactory.getDockerClient();
            jobClients.put(request.getJob().getId(), dockerClient);
        }catch (Exception e) {
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

    @Override
    public void launchContainer(JobDockerConfig request, StreamObserver<LaunchContainerResponse> responseObserver) {
        String containerId = null;
        DockerClient dockerClient = jobClients.get(request.getJob().getId());
        try {
            CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(request.getDockerImage());
            createContainerCmd.withBinds(request.getBindsList().stream().map(Bind::parse).collect(Collectors.toList()));
            createContainerCmd.withExposedPorts(request.getPortsList().stream().map(ExposedPort::parse).collect(Collectors.toList()));

            containerId = createContainerCmd.exec().getId();
            jobContainers.put(request.getJob().getId(), containerId);
            dockerClient.startContainerCmd(containerId).exec();

            dockerClient.logContainerCmd(containerId).withStdErr(true).withStdOut(true).withFollowStream(true).withTailAll()
                    .exec(new LogContainerResultCallback());

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

    @Override
    public void waitForContainerExit(ExitParams request, StreamObserver<ContainerExitCode> responseObserver) {
        DockerClient dockerClient = jobClients.get(request.getJob().getId());
        String containerId = jobContainers.get(request.getJob().getId());
        try {
            int exitCode = waitForContainer(dockerClient, containerId).awaitStatusCode();
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

    @Override
    public void waitForContainerExitWithTimeout(ExitWithTimeoutParams request, StreamObserver<ContainerExitCode> responseObserver) {
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

    private static boolean isValidUri(String test) {
        try {
            URI uri = URI.create(test);
            return uri.getScheme() != null;
        } catch (Exception unused) {
            return false;
        }
    }
}
