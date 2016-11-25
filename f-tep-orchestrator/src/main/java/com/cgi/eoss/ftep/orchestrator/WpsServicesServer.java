package com.cgi.eoss.ftep.orchestrator;

import com.cgi.eoss.ftep.model.internal.FtepJob;
import com.cgi.eoss.ftep.model.JobStatus;
import com.cgi.eoss.ftep.model.JobStep;
import com.cgi.eoss.ftep.model.rest.ApiEntity;
import com.cgi.eoss.ftep.model.rest.ResourceJob;
import com.cgi.eoss.ftep.wps.ApplicationLauncherGrpc;
import com.cgi.eoss.ftep.wps.ApplicationParams;
import com.cgi.eoss.ftep.wps.ApplicationResponse;
import com.google.common.collect.Iterables;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * <p>Server endpoint for the ApplicationLauncher RPC service.</p>
 */
@Slf4j
public class WpsServicesServer extends ApplicationLauncherGrpc.ApplicationLauncherImplBase {

    private final DownloadManager downloadManager;
    private final WorkerService workerService;
    private final JobStatusService jobStatusService;
    private final JobEnvironmentService jobEnvironmentService;

    public WpsServicesServer(DownloadManager downloadManager,
                             WorkerService workerService,
                             JobStatusService jobStatusService,
                             JobEnvironmentService jobEnvironmentService) {
        // TODO Distribute by configuring and connecting to these services via RPC
        this.downloadManager = downloadManager;
        this.workerService = workerService;
        this.jobStatusService = jobStatusService;
        this.jobEnvironmentService = jobEnvironmentService;
    }

    // TODO Extract common functionality between GUI Applications and Processing Services
    @Override
    public void launchApplication(ApplicationParams request, StreamObserver<ApplicationResponse> responseObserver) {
        // TODO Check coins and estimated cost

        JobEnvironment jobEnvironment;
        try {
            jobEnvironment = jobEnvironmentService.createEnvironment(request.getJobId());
        } catch (IOException e) {
            LOG.error("Failed to create working environment for job {}", request.getJobId());
            throw new RuntimeException(e);
        }

        FtepJob job = FtepJob.builder()
                .jobId(request.getJobId())
                .userId(request.getUserId())
                .serviceId(request.getServiceId())
                .status(JobStatus.CREATED)
                .workingDir(jobEnvironment.getWorkingDir())
                .inputDir(jobEnvironment.getInputDir())
                .outputDir(jobEnvironment.getOutputDir())
                .build();

        try {
            ApiEntity<ResourceJob> apiJob = jobStatusService.create(job.getJobId(), job.getUserId(), job.getServiceId());

            // Set up input data
            LOG.info("Downloading input data for {}", job.getJobId());
            apiJob.getResource().setStep(JobStep.DATA_FETCH.getText());
            jobStatusService.update(apiJob);
            downloadInputs(job, request.getInput());

            // Start the application
            apiJob.getResource().setStep(JobStep.PROCESSING.getText());
            jobStatusService.update(apiJob);
            String dockerImageTag = new FtepWpsServices().getImageFor(job.getServiceId());
            LOG.info("Launching docker image {} for {}", dockerImageTag, job.getJobId());

            Worker worker = workerService.getWorker();

            String guacamolePort = "8080/tcp";
            String containerId = worker.launchDockerContainer(DockerLaunchConfig.builder()
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
                throw new RuntimeException("Docker container returned with exit code " + exitCode);
            }

            LOG.info("Application terminated, collecting outputs from {}", job.getOutputDir());
            // TODO Collect outputs

            responseObserver.onNext(ApplicationResponse.newBuilder().setOutputUrl("").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to launch application; notifying gRPC client", e);
            responseObserver.onError(e);
        }
    }

    private void downloadInputs(FtepJob job, String inputUrl) {
        // TODO
    }

}
