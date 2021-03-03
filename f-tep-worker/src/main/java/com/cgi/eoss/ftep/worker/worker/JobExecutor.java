package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.queues.service.FtepQueueService;
import com.cgi.eoss.ftep.rpc.LocalWorker;
import com.cgi.eoss.ftep.rpc.worker.ContainerExit;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobError;
import com.cgi.eoss.ftep.rpc.worker.JobEvent;
import com.cgi.eoss.ftep.rpc.worker.JobEventType;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;

import java.util.HashMap;
import java.util.Map;

@Data
@Log4j2
public class JobExecutor implements Runnable {
    private Map<String, Object> messageHeaders = new HashMap<>();
    private final JobSpec jobSpec;
    private final FtepQueueService queueService;
    private final LocalWorker localWorker;
    private final String workerId;

    @Override
    public void run() {
        messageHeaders.put("workerId", workerId);
        messageHeaders.put("jobId", String.valueOf(jobSpec.getJob().getIntJobId()));
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(jobSpec)) {
            executeJob(jobSpec);
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

    private static CloseableThreadContext.Instance getJobLoggingContext(JobSpec jobSpec) {
        return CloseableThreadContext.push("F-TEP Worker Queue Dispatcher")
                .put("zooId", jobSpec.getJob().getId())
                .put("jobId", String.valueOf(jobSpec.getJob().getIntJobId()))
                .put("userId", jobSpec.getJob().getUserId())
                .put("serviceId", jobSpec.getJob().getServiceId());
    }
}
