package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.clouds.service.Node;
import com.cgi.eoss.ftep.clouds.service.NodeProvisioningException;
import com.cgi.eoss.ftep.queues.service.FtepQueueService;
import com.cgi.eoss.ftep.worker.metrics.QueueAverage;
import com.cgi.eoss.ftep.worker.metrics.QueueMetricsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Service for autoscaling the number of worker nodes based on queue length</p>
 */
@Log4j2
@Service
@ConditionalOnProperty(name = "ftep.worker.autoscaler.enabled", havingValue = "true", matchIfMissing = true)
public class FtepWorkerAutoscaler {

    private final JobEnvironmentService jobEnvironmentService;
    private final FtepWorkerNodeManager nodeManager;
    private final FtepQueueService queueService;
    private final QueueMetricsService queueMetricsService;

    // Scaling Configuration
    private static final long QUEUE_CHECK_INTERVAL_MS = 5L * 1000L;
    private static final long AUTOSCALER_INTERVAL_MS = 1L * 60L * 1000L;
    private static final long STATISTICS_WINDOW_MS = 2L * 60L * 1000L;

    private final int minWorkerNodes;
    private final int maxWorkerNodes;
    private final int maxJobsPerNode;

    private final long minSecondsBetweenScalingActions;
    private final long minimumHourFractionUptimeSeconds;
    private final String jobMessageSelector;

    private final Lock scalingLock = new ReentrantLock();

    private Instant lastAutoscalingActionTime = Instant.MIN;

    @Autowired
    public FtepWorkerAutoscaler(FtepWorkerNodeManager nodeManager, FtepQueueService queueService, QueueMetricsService queueMetricsService,
                                JobEnvironmentService jobEnvironmentService,
                                @Qualifier("minWorkerNodes") int minWorkerNodes,
                                @Qualifier("maxWorkerNodes") int maxWorkerNodes,
                                @Qualifier("maxJobsPerNode") int maxJobsPerNode,
                                @Qualifier("minSecondsBetweenScalingActions") long minSecondsBetweenScalingActions,
                                @Qualifier("minimumHourFractionUptimeSeconds") long minimumHourFractionUptimeSeconds,
                                @Qualifier("workerId") String workerId,
                                @Qualifier("restrictedWorker") boolean restrictedWorker
    ) {
        this.nodeManager = nodeManager;
        this.queueService = queueService;
        this.queueMetricsService = queueMetricsService;
        this.jobEnvironmentService = jobEnvironmentService;
        this.minWorkerNodes = minWorkerNodes;
        this.maxWorkerNodes = maxWorkerNodes;
        this.maxJobsPerNode = maxJobsPerNode;
        this.minSecondsBetweenScalingActions = minSecondsBetweenScalingActions;
        this.minimumHourFractionUptimeSeconds = minimumHourFractionUptimeSeconds;
        this.jobMessageSelector = restrictedWorker ? String.format("workerId = '%s'", workerId) : "";
    }

    @Scheduled(fixedRate = QUEUE_CHECK_INTERVAL_MS, initialDelay = 10000L)
    public void getCurrentQueueLength() {
        long queueLength = queueService.getQueueLength(FtepQueueService.jobQueueName, jobMessageSelector);
        queueMetricsService.updateMetric(queueLength, STATISTICS_WINDOW_MS / 1000L);
    }

    @Scheduled(fixedRate = AUTOSCALER_INTERVAL_MS, initialDelay = 10000L)
    public void decide() {
        if (scalingLock.tryLock()) {
            try {
                if (Instant.now().minusSeconds(minSecondsBetweenScalingActions).isBefore(lastAutoscalingActionTime)) {
                    return;
                }

                Set<Node> currentNodes = nodeManager.getCurrentNodes(FtepWorkerNodeManager.POOLED_WORKER_TAG);
                if (currentNodes.size() < minWorkerNodes) {
                    LOG.debug("Detected fewer nodes than the minimum ({}/{}), scaling back up", currentNodes.size(), minWorkerNodes);
                    scaleTo(minWorkerNodes);
                    return;
                }
                QueueAverage queueAverage = queueMetricsService.getMetrics(STATISTICS_WINDOW_MS / 1000L);
                double coverageFactor = 1.0 * QUEUE_CHECK_INTERVAL_MS / STATISTICS_WINDOW_MS;
                double coverage = queueAverage.getCount() * coverageFactor;
                if (coverage > 0.75) {
                    int averageLengthRounded = (int) Math.ceil(queueAverage.getAverageLength());
                    int scaleTarget = (int) Math.round(1.0 * averageLengthRounded / maxJobsPerNode);
                    LOG.debug("Avg queue length over the period is {}; scaling target is {}", queueAverage.getAverageLength(), scaleTarget);
                    scaleTo(scaleTarget);
                } else {
                    LOG.debug("Metrics coverage of {} not enough to take scaling decision", coverage);
                }
            } finally {
                scalingLock.unlock();
            }
        }
    }

    public void scaleTo(int target) {
        LOG.info("Scale target: {} nodes", target);
        int totalNodes = nodeManager.getCurrentNodes(FtepWorkerNodeManager.POOLED_WORKER_TAG).size();
        int freeNodes = nodeManager.getNumberOfFreeNodes(FtepWorkerNodeManager.POOLED_WORKER_TAG);
        LOG.debug("Current node balance: {} total nodes, {} free nodes", totalNodes, freeNodes);
        if (target > freeNodes) {
            Instant previousAutoScalingActionTime = lastAutoscalingActionTime;
            try {
                scaleUp(target - freeNodes);
                lastAutoscalingActionTime = Instant.now();
            } catch (NodeProvisioningException e) {
                LOG.debug("Autoscaling failed because of node provisioning exception", e);
                lastAutoscalingActionTime = previousAutoScalingActionTime;
            }
        } else if (target < freeNodes) {
            scaleDown(freeNodes - target);
            lastAutoscalingActionTime = Instant.now();
        } else {
            LOG.debug("No action needed as current free node count is equal to the target: {}", target);
        }
    }

    public void scaleUp(int numToScaleUp) throws NodeProvisioningException {
        LOG.info("Evaluating scale up of additional {} nodes", numToScaleUp);
        Set<Node> currentNodes = nodeManager.getCurrentNodes(FtepWorkerNodeManager.POOLED_WORKER_TAG);
        int scaleUpTarget = Math.min(currentNodes.size() + numToScaleUp, maxWorkerNodes);
        int adjustedScaleUpTarget = scaleUpTarget - currentNodes.size();
        LOG.info("Scaling up additional {} nodes. Max worker nodes are {}", adjustedScaleUpTarget, maxWorkerNodes);
        nodeManager.provisionNodes(adjustedScaleUpTarget, FtepWorkerNodeManager.POOLED_WORKER_TAG, jobEnvironmentService.getBaseDir());
    }

    public void scaleDown(int numToScaleDown) {
        LOG.info("Evaluating scale down of {} nodes", numToScaleDown);
        Set<Node> currentNodes = nodeManager.getCurrentNodes(FtepWorkerNodeManager.POOLED_WORKER_TAG);
        int scaleDownTarget = Math.max(currentNodes.size() - numToScaleDown, minWorkerNodes);
        int adjustedScaleDownTarget = currentNodes.size() - scaleDownTarget;
        LOG.info("Scaling down {} nodes. Min worker nodes are {}", adjustedScaleDownTarget, minWorkerNodes);
        nodeManager.destroyNodes(adjustedScaleDownTarget, FtepWorkerNodeManager.POOLED_WORKER_TAG, jobEnvironmentService.getBaseDir(), minimumHourFractionUptimeSeconds);
    }
}
