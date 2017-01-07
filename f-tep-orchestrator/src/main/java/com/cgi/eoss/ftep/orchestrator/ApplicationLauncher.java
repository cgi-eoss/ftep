package com.cgi.eoss.ftep.orchestrator;

import com.cgi.eoss.ftep.model.JobStatus;
import com.cgi.eoss.ftep.model.JobStep;
import com.cgi.eoss.ftep.model.internal.FtepJob;
import com.cgi.eoss.ftep.model.rest.ApiEntity;
import com.cgi.eoss.ftep.model.rest.ResourceJob;
import com.cgi.eoss.ftep.rpc.ApplicationLauncherGrpc;
import com.cgi.eoss.ftep.rpc.ApplicationParams;
import com.cgi.eoss.ftep.rpc.ApplicationResponse;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Server endpoint for the ApplicationLauncher RPC service.</p>
 */
@Slf4j
public class ApplicationLauncher extends ApplicationLauncherGrpc.ApplicationLauncherImplBase {

    private final WorkerService workerService;
    private final JobStatusService jobStatusService;
    private final ServiceDataService serviceDataService;

    public ApplicationLauncher(WorkerService workerService,
                               JobStatusService jobStatusService,
                               ServiceDataService serviceDataService) {
        // TODO Distribute by configuring and connecting to these services via RPC
        this.workerService = workerService;
        this.jobStatusService = jobStatusService;
        this.serviceDataService = serviceDataService;
    }

    // TODO Extract common functionality between GUI Applications and Processing Services
    @Override
    public void launchApplication(ApplicationParams request, StreamObserver<ApplicationResponse> responseObserver) {
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(request.getInputsList());

        // TODO Check coins and estimated cost

        FtepJob job = FtepJob.builder()
                .jobId(request.getJobId())
                .userId(request.getUserId())
                .serviceId(request.getServiceId())
                .status(JobStatus.CREATED)
                .build();

        ApiEntity<ResourceJob> apiJob = null;

        try {
            apiJob = jobStatusService.create(job.getJobId(), job.getUserId(), job.getServiceId());

            // Get a worker and instantiate the job workspace
            Worker worker = workerService.getWorker();
            JobEnvironment jobEnvironment = worker.createJobEnvironment(request.getJobId(), inputs);
            job.setWorkingDir(jobEnvironment.getWorkingDir());
            job.setInputDir(jobEnvironment.getInputDir());
            job.setOutputDir(jobEnvironment.getOutputDir());

            // Set up input data
            LOG.info("Downloading input data for {}", job.getJobId());
            apiJob.getResource().setStep(JobStep.DATA_FETCH.getText());
            jobStatusService.update(apiJob);

            worker.prepareInputs(inputs, job.getInputDir());

            // Start the application
            apiJob.getResource().setStep(JobStep.PROCESSING.getText());
            apiJob.getResource().setStatus(JobStatus.RUNNING.name());
            jobStatusService.update(apiJob);
            String dockerImageTag = serviceDataService.getImageFor(job.getServiceId());
            LOG.info("Launching docker image {} for {}", dockerImageTag, job.getJobId());

            String guacamolePort = "8080/tcp";
            String containerId = worker.launchDockerContainer(DockerLaunchConfig.builder()
                    .image(dockerImageTag)
                    .volume("/nobody/workDir")
                    .volume(job.getWorkingDir().getParent().toString())
                    .bind(job.getWorkingDir().toAbsolutePath().toString() + ":" + "/nobody/workDir")
                    .bind(job.getWorkingDir().getParent().toString() + ":" + job.getWorkingDir().getParent().toString())
                    .exposedPort(guacamolePort)
                    .defaultLogging(true)
                    .build());

            // TODO Stream logs from docker container to WPS (or direct to web client)

            // Retrieve the port binding from the container to publish as the GUI endpoint
            String guiEndpoint = Iterables.getOnlyElement(worker.getContainerPortBindings(containerId).get(guacamolePort));

            if (guiEndpoint == null) {
                throw new RuntimeException("Could not find GUI port on docker container");
            }

            LOG.info("Updating GUI endpoint for {} (job {}): {}", job.getServiceId(), job.getJobId(), guiEndpoint);
            apiJob.getResource().setGuiEndpoint(guiEndpoint);
            jobStatusService.update(apiJob);

            LOG.info("Application started");

            // Register timeout callback and wait for exit
            int exitCode = worker.waitForContainerExit(containerId, request.getTimeout());

            if (exitCode != 0) {
                throw new ServiceExecutionException("Docker container returned with exit code " + exitCode);
            }

            apiJob.getResource().setStep(JobStep.OUTPUT_LIST.getText());
            jobStatusService.update(apiJob);
            LOG.info("Application terminated, collecting outputs from {}", job.getOutputDir());
            // TODO Collect outputs for indexing in user's workspace

            responseObserver.onNext(ApplicationResponse.newBuilder().setOutputUrl("").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            if (apiJob != null) {
                jobStatusService.setJobInError(apiJob);
            }

            LOG.error("Failed to launch application; notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

}
