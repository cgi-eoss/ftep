package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.JobStatus;
import com.cgi.eoss.ftep.model.JobStep;
import com.cgi.eoss.ftep.model.ServiceType;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.rpc.FtepServiceLauncherGrpc;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.FtepServiceResponse;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.JobParam;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
import com.cgi.eoss.ftep.rpc.worker.LaunchContainerResponse;
import com.cgi.eoss.ftep.rpc.worker.PortBinding;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>Primary entry point for WPS services to launch in F-TEP.</p>
 * <p>Provides access to F-TEP data services and job distribution capability.</p>
 */
@Service
@Log4j2
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
        try (CloseableThreadContext.Instance ctc = CloseableThreadContext.push("F-TEP Service Orchestrator")
                .put("userId", userId).put("serviceId", serviceId).put("zooId", jobId)) {
            // TODO Allow re-use of existing JobConfig
            job = jobDataService.buildNew(jobId, userId, serviceId, inputs);
            ctc.put("jobId", String.valueOf(job.getId()));
            FtepService service = job.getConfig().getService();

            checkCost(job.getConfig());

            // TODO Determine WorkerEnvironment from service parameters
            FtepWorkerGrpc.FtepWorkerBlockingStub worker = workerFactory.getWorker(WorkerEnvironment.LOCAL);

            // Prepare inputs
            LOG.info("Downloading input data for {}", jobId);
            com.cgi.eoss.ftep.rpc.Job rpcJob = com.cgi.eoss.ftep.rpc.Job.newBuilder().setId(jobId).build();
            job.setStartTime(LocalDateTime.now());
            job.setStatus(JobStatus.RUNNING);
            job.setStage(JobStep.DATA_FETCH.getText());
            jobDataService.save(job);
            JobEnvironment jobEnvironment = worker.prepareEnvironment(JobInputs.newBuilder()
                    .setJob(rpcJob)
                    .addAllInputs(request.getInputsList())
                    .build());

            // Configure container
            String dockerImageTag = service.getDockerTag();
            JobDockerConfig.Builder dockerConfigBuilder = JobDockerConfig.newBuilder()
                    .setJob(rpcJob)
                    .setServiceName(serviceId)
                    .setDockerImage(dockerImageTag)
                    .addBinds(jobEnvironment.getWorkingDir() + ":" + "/nobody/workDir")
                    .addBinds(Paths.get(jobEnvironment.getWorkingDir()).getParent().toString() + ":" + Paths.get(jobEnvironment.getWorkingDir()).getParent().toString());
            if (service.getType() == ServiceType.APPLICATION) {
                dockerConfigBuilder.addPorts(GUACAMOLE_PORT);
            }
            LOG.info("Launching docker image {} for {}", dockerImageTag, jobId);
            job.setStage(JobStep.PROCESSING.getText());
            jobDataService.save(job);
            LaunchContainerResponse unused = worker.launchContainer(dockerConfigBuilder.build());

            // TODO Implement async service command execution

            LOG.info("Job {} ({}) launched for service: {}", job.getId(), jobId, service.getName());

            // Update GUI endpoint URL for client access
            if (service.getType() == ServiceType.APPLICATION) {
                PortBinding portBinding = worker.getPortBindings(rpcJob).getBindingsList().stream()
                        .filter(b -> b.getPortDef().equals(GUACAMOLE_PORT))
                        .findFirst()
                        .orElseThrow(() -> new ServiceExecutionException("Could not find GUI port on docker container: " + jobId));

                String guiUrl = portBinding.getBinding();

                LOG.info("Updating GUI URL for job {} ({}): {}", jobId, job.getConfig().getService().getName(), guiUrl);
                job.setGuiUrl(guiUrl);
                jobDataService.save(job);
            }

            // Wait for exit, with timeout if necessary
            ContainerExitCode exitCode;
            if (inputs.containsKey(TIMEOUT_PARAM)) {
                int timeout = Integer.parseInt(Iterables.getOnlyElement(inputs.get(TIMEOUT_PARAM)));
                exitCode = worker.waitForContainerExitWithTimeout(ExitWithTimeoutParams.newBuilder().setJob(rpcJob).setTimeout(timeout).build());
            } else {
                exitCode = worker.waitForContainerExit(ExitParams.newBuilder().setJob(rpcJob).build());
            }

            if (exitCode.getExitCode() != 0) {
                throw new ServiceExecutionException("Docker container returned with exit code " + exitCode);
            }

            // TODO Collect outputs
            job.setStage(JobStep.OUTPUT_LIST.getText());
            jobDataService.save(job);
            List<JobParam> outputs = ImmutableList.of();

            responseObserver.onNext(FtepServiceResponse.newBuilder()
                    .addAllOutputs(outputs)
                    .build());
            responseObserver.onCompleted();

            job.setStatus(JobStatus.COMPLETED);
            job.setEndTime(LocalDateTime.now());
            jobDataService.save(job);
        } catch (Exception e) {
            if (job != null) {
                job.setStatus(JobStatus.ERROR);
                job.setEndTime(LocalDateTime.now());
                jobDataService.save(job);
            }

            LOG.error("Failed to run processor; notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    private void checkCost(JobConfig jobConfig) {
        // TODO Determine coin cost of config and compare with user account
    }

}
