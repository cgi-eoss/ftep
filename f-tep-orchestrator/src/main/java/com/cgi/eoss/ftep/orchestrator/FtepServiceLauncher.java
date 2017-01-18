package com.cgi.eoss.ftep.orchestrator;

import com.cgi.eoss.ftep.model.FtepJob;
import com.cgi.eoss.ftep.model.JobStatus;
import com.cgi.eoss.ftep.model.JobStep;
import com.cgi.eoss.ftep.model.enums.ServiceType;
import com.cgi.eoss.ftep.orchestrator.worker.JobEnvironment;
import com.cgi.eoss.ftep.orchestrator.worker.Worker;
import com.cgi.eoss.ftep.orchestrator.worker.WorkerEnvironment;
import com.cgi.eoss.ftep.orchestrator.worker.WorkerFactory;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.rpc.FtepServiceLauncherGrpc;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.FtepServiceResponse;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.Param;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * <p>Primary entrypoint for WPS services to launch in F-TEP.</p>
 * <p>Provides access to F-TEP data services and job distribution capability.</p>
 */
@Service
@Slf4j
public class FtepServiceLauncher extends FtepServiceLauncherGrpc.FtepServiceLauncherImplBase {

    private static final String GUACAMOLE_PORT = "8080/tcp";
    private static final String TIMEOUT_PARAM = "timeout";

    private final WorkerFactory workerFactory;
    private final JobDataService jobDataService;

    @Autowired
    public FtepServiceLauncher(WorkerFactory workerFactory, JobDataService jobDataService) {
        this.workerFactory = workerFactory;
        this.jobDataService = jobDataService;
    }

    @Override
    public void launchService(FtepServiceParams request, StreamObserver<FtepServiceResponse> responseObserver) {
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(request.getInputsList());

        String jobId = request.getJobId();
        String userId = request.getUserId();
        String serviceId = request.getServiceId();

        FtepJob job = null;
        try {
            job = jobDataService.buildNew(jobId, userId, serviceId);

            checkCost(job);

            // TODO Determine WorkerEnvironment from service parameters
            Worker worker = workerFactory.getWorker(WorkerEnvironment.LOCAL);
            JobEnvironment jobEnvironment = worker.createJobEnvironment(job.getJobId(), inputs);

            prepareInputs(inputs, job, worker, jobEnvironment);

            // TODO Use a proper container ID field, not "description"
            String dockerImageTag = job.getService().getDescription();

            DockerLaunchConfig.DockerLaunchConfigBuilder dockerConfigBuilder = DockerLaunchConfig.builder()
                    .image(dockerImageTag)
                    .volume("/nobody/workDir")
                    .volume(jobEnvironment.getWorkingDir().getParent().toString())
                    .bind(jobEnvironment.getWorkingDir().toAbsolutePath().toString() + ":" + "/nobody/workDir")
                    .bind(jobEnvironment.getWorkingDir().getParent().toString() + ":" + jobEnvironment.getWorkingDir().getParent().toString())
                    .defaultLogging(true);

            if (job.getService().getKind() == ServiceType.APPLICATION) {
                dockerConfigBuilder.exposedPort(GUACAMOLE_PORT);
            }

            String containerId = launchContainer(job, worker, dockerConfigBuilder.build());
            // TODO Stream logs from docker container to WPS (or direct to web client)
            LOG.info("Job {} ({}) launched (full ID: {})", job.getId(), job.getService().getName(), job.getJobId());

            // Wait for exit, with timeout if necessary
            int exitCode;
            if (job.getService().getKind() == ServiceType.APPLICATION) {
                updateGuiEndpoint(job, worker, containerId);
                int timeout = Integer.parseInt(Iterables.getOnlyElement(inputs.get(TIMEOUT_PARAM)));
                exitCode = worker.waitForContainerExit(containerId, timeout);
            } else {
                exitCode = worker.waitForContainerExit(containerId);
            }

            if (exitCode != 0) {
                throw new ServiceExecutionException("Docker container returned with exit code " + exitCode);
            }

            executeService(job, containerId);

            List<Param> outputs = collectOutputs(job);

            responseObserver.onNext(FtepServiceResponse.newBuilder()
                    .addAllOutputs(outputs)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            if (job != null) {
                job.setStatus(JobStatus.ERROR);
                jobDataService.save(job);
            }

            LOG.error("Failed to run processor; notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    private void checkCost(FtepJob job) {
        // TODO Determine coin cost of job and compare with user account
    }

    private void prepareInputs(Multimap<String, String> inputs, FtepJob job, Worker worker, JobEnvironment jobEnvironment) throws IOException {
        LOG.info("Downloading input data for {}", job.getJobId());
        job.setStatus(JobStatus.RUNNING);
        job.setStep(JobStep.DATA_FETCH.getText());
        jobDataService.save(job);
        worker.prepareInputs(inputs, jobEnvironment.getInputDir());
    }

    private String launchContainer(FtepJob job, Worker worker, DockerLaunchConfig dockerLaunchConfig) {
        job.setStep(JobStep.PROCESSING.getText());
        jobDataService.save(job);

        LOG.info("Launching docker image {} for {}", dockerLaunchConfig.getImage(), job.getJobId());
        String containerId = worker.launchDockerContainer(dockerLaunchConfig);
        LOG.info("Processor launched: (job {}) {}", job.getJobId(), job.getService().getName());

        return containerId;
    }

    private void updateGuiEndpoint(FtepJob job, Worker worker, String containerId) {
        // Retrieve the port binding from the container to publish as the GUI endpoint
        String guiEndpoint = Iterables.getOnlyElement(worker.getContainerPortBindings(containerId).get(GUACAMOLE_PORT), null);

        if (guiEndpoint == null) {
            throw new ServiceExecutionException("Could not find GUI port on docker container for job " + job.getId());
        }

        LOG.info("Updating GUI endpoint for {} (job {}): {}", job.getService().getName(), job.getJobId(), guiEndpoint);
        job.setGuiEndPoint(guiEndpoint);
        jobDataService.save(job);
    }

    private void executeService(FtepJob job, String containerId) {
        // TODO Implement async services-as-configuration
    }

    private List<Param> collectOutputs(FtepJob job) {
        job.setStep(JobStep.OUTPUT_LIST.getText());
        jobDataService.save(job);

        // TODO Implement

        return ImmutableList.of();
    }

}
