package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.queues.service.FtepQueueService;
import com.cgi.eoss.ftep.rpc.LocalWorker;
import com.cgi.eoss.ftep.rpc.worker.ContainerStatus;
import com.cgi.eoss.ftep.rpc.worker.DockerImageConfig;
import com.cgi.eoss.ftep.rpc.worker.GetNodesRequest;
import com.cgi.eoss.ftep.rpc.worker.GetResumableJobsRequest;
import com.cgi.eoss.ftep.rpc.worker.JobContainer;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import com.cgi.eoss.ftep.rpc.worker.Node;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * <p>Service for building and executing F-TEP (WPS) services inside Docker containers.</p>
 */
@Log4j2
@Service
public class FtepWorkerDispatcher {

    private final FtepQueueService queueService;
    private final String workerId;
    private final LocalWorker localWorker;
    private final FtepWorkerNodeManager nodeManager;
    private final String jobMessageSelector;

    private static final long QUEUE_INITIAL_DELAY = 10L * 1000L;
    private static final long QUEUE_SCHEDULER_INTERVAL_MS = 10L * 1000L;
    private static final long QUEUE_TIMEOUT = 100L;

    private final ListeningExecutorService jobExecutorService;

    @Autowired
    public FtepWorkerDispatcher(FtepQueueService queueService, LocalWorker localWorker,
                                @Qualifier("workerId") String workerId,
                                @Qualifier("restrictedWorker") boolean restrictedWorker,
                                FtepWorkerNodeManager nodeManager) {
        this.queueService = queueService;
        this.localWorker = localWorker;
        this.workerId = workerId;
        this.nodeManager = nodeManager;
        this.jobMessageSelector = restrictedWorker ? String.format("workerId = '%s'", workerId) : "";
        this.jobExecutorService = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool(nodeManager.getMaxConcurrency()));
    }

    public void recoverJobs() {
        // Get all nodes
        List<Node> nodes = localWorker.getNodes(GetNodesRequest.getDefaultInstance()).getNodesList();

        for (Node node : nodes) {

            // Query all running/finished job containers that this worker had started on the given node
            List<JobContainer> resumableJobContainers = localWorker
                    .getResumableJobs(GetResumableJobsRequest.newBuilder().setWorkerId(workerId).setNodeId(node.getId()).build())
                    .getJobsList();

            // Resume each job on a separate thread
            for (JobContainer jobContainer : resumableJobContainers) {
                JobExecutor jobExecutor = new JobExecutor(queueService, localWorker, workerId, jobContainer.getContainerStatus());
                jobExecutor.setJob(jobContainer.getJob());
                jobExecutorService.submit(jobExecutor);
            }
        }
    }

    @Scheduled(fixedRate = QUEUE_SCHEDULER_INTERVAL_MS, initialDelay = QUEUE_INITIAL_DELAY)
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

    /**
     * A method to poll the Docker image build queue once every 10 seconds.
     * If any new image build tasks are queued, dequeue them and create a new thread for handling each image build task
     */
    @Scheduled(fixedRate = QUEUE_SCHEDULER_INTERVAL_MS, initialDelay = QUEUE_INITIAL_DELAY)
    public void getImageBuildTasks() {
        long queueLength = queueService.getQueueLength(FtepQueueService.dockerImageBuildsQueueName);
        if (queueLength > 0) {
            DockerImageConfig dockerImageConfig;
            while ((dockerImageConfig = getNextDockerImageConfig()) != null) {
                ImageBuilder imageBuilder = new ImageBuilder(queueService, dockerImageConfig, localWorker);
                jobExecutorService.submit(imageBuilder);
            }
        }
    }

    private void consumeJobQueue() {
        LOG.debug("Consuming job queue until no remaining node capacity or queue is empty");
        JobSpec nextJobSpec;
        while (nodeManager.hasCapacity() && (nextJobSpec = getNextJobSpec()) != null) {
            LOG.info("Dequeued job {}", nextJobSpec.getJob().getId());
            nodeManager.reserveNodeForJob(nextJobSpec.getJob().getId());
            JobExecutor jobExecutor = new JobExecutor(queueService, localWorker, workerId, ContainerStatus.NEW);
            jobExecutor.setJobSpec(nextJobSpec.toBuilder().setWorker(JobSpec.Worker.newBuilder().setId(workerId).build()).build());
            jobExecutorService.submit(jobExecutor);
        }
    }

    private JobSpec getNextJobSpec() {
        return (JobSpec) queueService.receiveObjectWithTimeout(FtepQueueService.jobQueueName, jobMessageSelector, QUEUE_TIMEOUT);
    }

    private DockerImageConfig getNextDockerImageConfig() {
        return (DockerImageConfig) queueService.receiveObjectWithTimeout(FtepQueueService.dockerImageBuildsQueueName, QUEUE_TIMEOUT);
    }

}
