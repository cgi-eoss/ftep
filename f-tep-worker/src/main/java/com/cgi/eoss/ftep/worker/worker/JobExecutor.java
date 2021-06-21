package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.queues.service.FtepQueueService;
import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.LocalWorker;
import com.cgi.eoss.ftep.rpc.worker.ContainerExit;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.ContainerStatus;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.GetJobEnvironmentRequest;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobError;
import com.cgi.eoss.ftep.rpc.worker.JobEvent;
import com.cgi.eoss.ftep.rpc.worker.JobEventType;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import com.cgi.eoss.ftep.rpc.worker.TerminateJobRequest;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@Log4j2
public class JobExecutor implements Runnable {
    private Map<String, Object> messageHeaders = new HashMap<>();
    private Job job;
    private JobSpec jobSpec;
    private final FtepQueueService queueService;
    private final LocalWorker localWorker;
    private final String workerId;
    private final ContainerStatus containerStatus;

    @Override
    public void run() {
        messageHeaders.put("workerId", workerId);
        messageHeaders.put("zooId", job != null ? job.getId() : jobSpec.getJob().getId());
        messageHeaders.put("jobId", String.valueOf(job != null ? job.getIntJobId() : jobSpec.getJob().getIntJobId()));
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(Optional.ofNullable(job).orElseGet(() -> jobSpec.getJob()))) {
            switch (containerStatus) {
                case NEW:
                    executeJob(jobSpec);
                    break;
                case RUNNING:
                case COMPLETED:
                    resumeJob(job);
                    break;
                case TIMEOUT:
                    terminateJob(job);
                    break;
            }
        }
    }

    private void jobUpdate(Object object) {
        queueService.sendObject(FtepQueueService.jobUpdatesQueueName, messageHeaders, object);
    }

    // Entry point after Job is dequeued
    private void executeJob(JobSpec jobSpec) {
        try {
            jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.DATA_FETCHING_STARTED).build());
            JobInputs jobInputs = JobInputs.newBuilder().setJob(jobSpec.getJob()).addAllInputs(jobSpec.getInputsList()).build();
            //eventListener needs output directory for containerExit event
            JobEnvironment jobEnvironment = localWorker.prepareInputs(jobInputs);
            jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.DATA_FETCHING_COMPLETED).build());

            localWorker.launchContainer(jobSpec);
            jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.PROCESSING_STARTED).build());

            int exitCode;
            if (jobSpec.getHasTimeout()) {
                ExitWithTimeoutParams exitRequest
                        = ExitWithTimeoutParams.newBuilder().setJob(jobSpec.getJob()).setTimeout(jobSpec.getTimeoutValue()).build();
                ContainerExitCode containerExitCode = localWorker.waitForContainerExitWithTimeout(exitRequest);
                exitCode = containerExitCode.getExitCode();
            } else {
                ExitParams exitRequest = ExitParams.newBuilder().setJob(jobSpec.getJob()).build();
                ContainerExitCode containerExitCode = localWorker.waitForContainerExit(exitRequest);
                exitCode = containerExitCode.getExitCode();
            }

            jobUpdate(ContainerExit.newBuilder().setExitCode(exitCode).setJobEnvironment(jobEnvironment).build());
        } catch (Exception e) {
            LOG.error("Error executing job ", e);
            jobUpdate(JobError.newBuilder().setErrorDescription(e.getMessage() != null ? e.getMessage() : "Unknown error").build());
        } finally {
            localWorker.cleanUp(jobSpec.getJob());
        }
    }

    private void resumeJob(Job job) {
        try {
            LOG.info("Resuming job {}", job.getIntJobId());
            ExitParams exitRequest = ExitParams.newBuilder().setJob(job).build();
            int exitCode = localWorker.waitForContainerExit(exitRequest).getExitCode();

            GetJobEnvironmentRequest getJobEnvironmentRequest = GetJobEnvironmentRequest.newBuilder().setJob(job).build();
            JobEnvironment jobEnvironment = localWorker.getExistingJobEnvironment(getJobEnvironmentRequest);
            jobUpdate(ContainerExit.newBuilder().setExitCode(exitCode).setJobEnvironment(jobEnvironment).build());
        } catch (Exception e) {
            LOG.error("Error resuming job ", e);
            jobUpdate(JobError.newBuilder().setErrorDescription(e.getMessage() != null ? e.getMessage() : "Unknown error").build());
        } finally {
            localWorker.cleanUp(job);
        }
    }

    private void terminateJob(Job job) {
        try {
            LOG.info("Terminating timed out job {}", job.getIntJobId());
            int exitCode = localWorker.terminateJob(TerminateJobRequest.newBuilder().setJob(job).build()).getExitCode();

            GetJobEnvironmentRequest getJobEnvironmentRequest = GetJobEnvironmentRequest.newBuilder().setJob(job).build();
            JobEnvironment jobEnvironment = localWorker.getExistingJobEnvironment(getJobEnvironmentRequest);
            jobUpdate(ContainerExit.newBuilder().setExitCode(exitCode).setJobEnvironment(jobEnvironment).build());
        } catch (Exception e) {
            LOG.error("Error terminating job ", e);
            jobUpdate(JobError.newBuilder().setErrorDescription(e.getMessage() != null ? e.getMessage() : "Unknown error").build());
        } finally {
            localWorker.cleanUp(job);
        }
    }

    private static CloseableThreadContext.Instance getJobLoggingContext(Job job) {
        return CloseableThreadContext.push("F-TEP Worker Job Executor")
                .put("zooId", job.getId())
                .put("jobId", String.valueOf(job.getIntJobId()))
                .put("userId", job.getUserId())
                .put("serviceId", job.getServiceId());
    }
}
