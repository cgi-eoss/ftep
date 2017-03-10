package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.DockerContainer;
import com.cgi.eoss.ftep.rpc.worker.DockerContainerConfig;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class FtepWorker extends FtepWorkerGrpc.FtepWorkerImplBase {

    private final DockerClientFactory dockerClientFactory;
    private final JobEnvironmentService jobEnvironmentService;
    private final ServiceInputOutputManager inputOutputManager;

    // Track which DockerClient is used for which container
    private final Map<String, DockerClient> containerClients = new HashMap<>();

    @Autowired
    public FtepWorker(DockerClientFactory dockerClientFactory, JobEnvironmentService jobEnvironmentService, ServiceInputOutputManager inputOutputManager) {
        this.dockerClientFactory = dockerClientFactory;
        this.jobEnvironmentService = jobEnvironmentService;
        this.inputOutputManager = inputOutputManager;
    }

    @Override
    public void prepareInputs(JobInputs request, StreamObserver<JobEnvironment> responseObserver) {
        try {
            Multimap<String, String> inputs = GrpcUtil.paramsListToMap(request.getInputsList());

            // Create workspace directories and input parameters file
            com.cgi.eoss.ftep.worker.worker.JobEnvironment jobEnv = jobEnvironmentService.createEnvironment(request.getJobId(), inputs);

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
            LOG.error("Failed to prepare job inputs for {}", request.getJobId(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    @Override
    public void launchContainer(DockerContainerConfig request, StreamObserver<DockerContainer> responseObserver) {
        String containerId = null;
        try {
            DockerClient dockerClient = dockerClientFactory.getDockerClient();

            CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(request.getDockerImage());
            createContainerCmd.withBinds(request.getBindsList().stream().map(Bind::parse).collect(Collectors.toList()));
            createContainerCmd.withExposedPorts(request.getPortsList().stream().map(ExposedPort::parse).collect(Collectors.toList()));

            containerId = createContainerCmd.exec().getId();
            dockerClient.startContainerCmd(containerId).exec();

            containerClients.put(containerId, dockerClient);

            // Enable container logging via slf4J by default
            dockerClient.logContainerCmd(containerId).withStdErr(true).withStdOut(true).withFollowStream(true).withTailAll()
                    .exec(new LogContainerResultCallback());

            responseObserver.onNext(DockerContainer.newBuilder().setId(containerId).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to launch docker container {}", request.getDockerImage(), e);
            if (!Strings.isNullOrEmpty(containerId)) {
                removeContainer(containerId);
            }
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    @Override
    public void waitForContainerExit(ExitParams request, StreamObserver<ContainerExitCode> responseObserver) {
        String containerId = request.getContainer().getId();
        try {
            int exitCode = waitForContainer(containerId).awaitStatusCode();
            responseObserver.onNext(ContainerExitCode.newBuilder().setExitCode(exitCode).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to properly wait for container exit: {}", containerId, e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        } finally {
            removeContainer(containerId);
        }
    }

    @Override
    public void waitForContainerExitWithTimeout(ExitWithTimeoutParams request, StreamObserver<ContainerExitCode> responseObserver) {
        String containerId = request.getContainer().getId();
        try {
            int exitCode = waitForContainer(containerId).awaitStatusCode(request.getTimeout(), TimeUnit.HOURS);
            responseObserver.onNext(ContainerExitCode.newBuilder().setExitCode(exitCode).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to properly wait for container exit: {}", containerId, e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        } finally {
            removeContainer(containerId);
        }
    }

    private WaitContainerResultCallback waitForContainer(String containerId) {
        return containerClients.get(containerId).waitContainerCmd(containerId).exec(new WaitContainerResultCallback());
    }

    private void removeContainer(String containerId) {
        try {
            LOG.info("Removing container {}", containerId);
            containerClients.get(containerId).removeContainerCmd(containerId).exec();
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
