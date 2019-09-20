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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * <p>Service for executing F-TEP (WPS) services inside Docker containers.</p>
 */
@Log4j2
@Service
public class FtepWorkerDispatcher {

    private final FtepQueueService queueService;
    private final String workerId;
    private final boolean restrictedWorker;
    private final LocalWorker localWorker;
    private final FtepWorkerNodeManager nodeManager;
    private final String jobMessageSelector;

    private static final long QUEUE_SCHEDULER_INTERVAL_MS = 10L * 1000L;
    private static final Function<String, String> RANDOM_DIR_NAME = prefix -> prefix + UUID.randomUUID();

    @Autowired
    public FtepWorkerDispatcher(FtepQueueService queueService, LocalWorker localWorker,
                                @Qualifier("workerId") String workerId,
                                @Qualifier("restrictedWorker") boolean restrictedWorker,
                                FtepWorkerNodeManager nodeManager) {
        this.queueService = queueService;
        this.localWorker = localWorker;
        this.workerId = workerId;
        this.restrictedWorker = restrictedWorker;
        this.nodeManager = nodeManager;
        this.jobMessageSelector = restrictedWorker ? String.format("workerId = '%s'", workerId) : "";
    }

    @Scheduled(fixedRate = QUEUE_SCHEDULER_INTERVAL_MS, initialDelay = 10000L)
    public void getNewJobs() {
        LOG.debug("Checking for available jobs in the queue");

        long queueLength = queueService.getQueueLength(FtepQueueService.jobQueueName, jobMessageSelector);
        if (queueLength > 0) {
            LOG.debug("Found {} queued jobs, checking for available node capacity", queueLength);
            if (nodeManager.hasCapacity()) {
                consumeJobQueue();
            } else {
                LOG.debug("No nodes have capacity to execute queued jobs");
            }
        } else {
            LOG.debug("Job queue currently empty");
        }
    }

    private void consumeJobQueue() {
        LOG.debug("Consuming job queue until no remaining node capacity or queue is empty");
        JobSpec nextJobSpec;
        while (nodeManager.hasCapacity() && (nextJobSpec = getNextJobSpec()) != null) {
            LOG.info("Dequeued job {}", nextJobSpec.getJob().getId());
            nodeManager.reserveNodeForJob(nextJobSpec.getJob().getId());
            Thread t = new Thread(new JobExecutor(nextJobSpec, queueService));
            t.start();
        }
    }

    private JobSpec getNextJobSpec() {
        return (JobSpec) queueService.receiveObjectWithTimeout(FtepQueueService.jobQueueName, jobMessageSelector, 100);
    }

    public interface JobUpdateListener {
        void jobUpdate(Object object);
    }

    @Data
    public class JobExecutor implements Runnable, JobUpdateListener {
        Map<String, Object> messageHeaders;
        private final JobSpec jobSpec;
        private final FtepQueueService queueService;

        @Override
        public void run() {
            messageHeaders = new HashMap<>();
            messageHeaders.put("workerId", workerId);
            messageHeaders.put("jobId", String.valueOf(jobSpec.getJob().getIntJobId()));
            try (CloseableThreadContext.Instance ctc = getJobLoggingContext(jobSpec)) {
                executeJob(jobSpec, this);
            }
        }

        @Override
        public void jobUpdate(Object object) {
            queueService.sendObject(FtepQueueService.jobUpdatesQueueName, messageHeaders, object);
        }
    }

    // Entry point after Job is dequeued
    private void executeJob(JobSpec jobSpec, JobUpdateListener jobUpdateListener) {
        try {
            jobUpdateListener.jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.DATA_FETCHING_STARTED).build());
            JobInputs jobInputs = JobInputs.newBuilder().setJob(jobSpec.getJob()).addAllInputs(jobSpec.getInputsList()).build();
            //eventListener needs output directory for containerExit event
            JobEnvironment jobEnvironment = localWorker.prepareInputs(jobInputs);
            jobUpdateListener.jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.DATA_FETCHING_COMPLETED).build());

            localWorker.launchContainer(jobSpec);
            jobUpdateListener.jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.PROCESSING_STARTED).build());

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

            jobUpdateListener.jobUpdate(ContainerExit.newBuilder().setExitCode(exitCode).setJobEnvironment(jobEnvironment).build());
        } catch (Exception e) {
            LOG.error("Error executing job ", e);
            jobUpdateListener.jobUpdate(JobError.newBuilder().setErrorDescription(e.getMessage() != null ? e.getMessage() : "Unknown error").build());
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
