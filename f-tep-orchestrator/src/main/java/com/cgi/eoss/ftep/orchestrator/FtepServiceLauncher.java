package com.cgi.eoss.ftep.orchestrator;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.JobStatus;
import com.cgi.eoss.ftep.model.JobStep;
import com.cgi.eoss.ftep.model.ServiceType;
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
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * <p>Primary entrypoint for WPS services to launch in F-TEP.</p>
 * <p>Provides access to F-TEP data services and job distribution capability.</p>
 */
@Service
@Slf4j
@GRpcService
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

        Job job = null;
        try {
            // TODO Allow re-use of existing JobConfig
            job = jobDataService.buildNew(jobId, userId, serviceId, inputs);
            FtepService service = job.getConfig().getService();

            checkCost(job.getConfig());

            // TODO Determine WorkerEnvironment from service parameters
            Worker worker = workerFactory.getWorker(WorkerEnvironment.LOCAL);
            JobEnvironment jobEnvironment = worker.createJobEnvironment(job.getExtId(), inputs);

            prepareInputs(inputs, job, worker, jobEnvironment);

            String dockerImageTag = service.getDockerTag();
            DockerLaunchConfig.DockerLaunchConfigBuilder dockerConfigBuilder = DockerLaunchConfig.builder()
                    .image(dockerImageTag)
                    .volume("/nobody/workDir")
                    .volume(jobEnvironment.getWorkingDir().getParent().toString())
                    .bind(jobEnvironment.getWorkingDir().toAbsolutePath().toString() + ":" + "/nobody/workDir")
                    .bind(jobEnvironment.getWorkingDir().getParent().toString() + ":" + jobEnvironment.getWorkingDir().getParent().toString())
                    .defaultLogging(true);

            if (service.getType() == ServiceType.APPLICATION) {
                dockerConfigBuilder.exposedPort(GUACAMOLE_PORT);
            }

            String containerId = launchContainer(job, worker, dockerConfigBuilder.build());
            // TODO Stream logs from docker container to WPS (or direct to web client)
            LOG.info("Job {} ({}) launched for service: {}", job.getId(), job.getExtId(), service.getName());

            // Wait for exit, with timeout if necessary
            int exitCode;
            if (service.getType() == ServiceType.APPLICATION) {
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

            job.setStatus(JobStatus.COMPLETED);
            jobDataService.save(job);
        } catch (Exception e) {
            if (job != null) {
                job.setStatus(JobStatus.ERROR);
                jobDataService.save(job);
            }

            LOG.error("Failed to run processor; notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    private void checkCost(JobConfig jobConfig) {
        // TODO Determine coin cost of config and compare with user account
    }

    private void prepareInputs(Multimap<String, String> inputs, Job job, Worker worker, JobEnvironment jobEnvironment) throws IOException {
        LOG.info("Downloading input data for {}", job.getExtId());
        job.setStartTime(ZonedDateTime.now());
        job.setStatus(JobStatus.RUNNING);
        job.setStage(JobStep.DATA_FETCH.getText());
        jobDataService.save(job);
        worker.prepareInputs(inputs, jobEnvironment.getInputDir());
    }

    private String launchContainer(Job job, Worker worker, DockerLaunchConfig dockerLaunchConfig) {
        job.setStage(JobStep.PROCESSING.getText());
        jobDataService.save(job);

        LOG.info("Launching docker image {} for {}", dockerLaunchConfig.getImage(), job.getExtId());
        return worker.launchDockerContainer(dockerLaunchConfig);
    }

    private void updateGuiEndpoint(Job job, Worker worker, String containerId) {
        // Retrieve the port binding from the container to publish as the GUI endpoint
        String guiUrl = Iterables.getOnlyElement(worker.getContainerPortBindings(containerId).get(GUACAMOLE_PORT), null);

        if (guiUrl == null) {
            throw new ServiceExecutionException("Could not find GUI port on docker container for config " + job.getId());
        }

        LOG.info("Updating GUI URL for job {} ({}): {}", job.getExtId(), job.getConfig().getService().getName(), guiUrl);
        job.setGuiUrl(guiUrl);
        jobDataService.save(job);
    }

    private void executeService(Job job, String containerId) {
        // TODO Implement async services-as-configuration
    }

    private List<Param> collectOutputs(Job job) {
        job.setStage(JobStep.OUTPUT_LIST.getText());
        jobDataService.save(job);

        // TODO Implement

        return ImmutableList.of();
    }

}
