package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.clouds.service.Node;
import com.cgi.eoss.ftep.clouds.service.NodeFactory;
import com.cgi.eoss.ftep.clouds.service.StorageProvisioningException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Log4j2
public class FtepWorkerNodeManager {

    private final int maxJobsPerNode;
    private final Path dataBaseDir;
    private final NodeFactory nodeFactory;
    private final ListeningExecutorService provisioningExecutorService = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool(4));
    private final ListeningExecutorService destroyingAndCallbackExecutorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    // Track which Node is used for each job
    private final SetMultimap<Node, String> nodeJobs = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    // Track dynamic block volumes attached to nodes
    private final SetMultimap<String, String> jobVolumes = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    public static final String POOLED_WORKER_TAG = "pooled-worker-node";
    public static final String DEDICATED_WORKER_TAG = "dedicated-worker-node";

    private final int maxNodes;

    public FtepWorkerNodeManager(NodeFactory nodeFactory, Path dataBaseDir, int maxNodes, int maxJobsPerNode) {
        this.nodeFactory = nodeFactory;
        this.dataBaseDir = dataBaseDir;
        this.maxNodes = maxNodes;
        this.maxJobsPerNode = maxJobsPerNode;
    }

    public int getMaxConcurrency() {
        return maxNodes * maxJobsPerNode;
    }

    public Set<Node> getCurrentNodes() {
        return nodeFactory.getCurrentNodes();
    }

    public Set<Node> getCurrentNodes(String tag) {
        return nodeFactory.getCurrentNodes(tag);
    }

    public int getNumberOfFreeNodes(String tag) {
        return nodeFactory.getCurrentNodes(tag).stream().filter(n -> nodeJobs.get(n).isEmpty()).collect(toSet()).size();
    }

    @Synchronized
    public ListenableFuture<List<Optional<Node>>> provisionNodes(int provisionTarget, String tag, Path baseDir) {
        List<ListenableFuture<Optional<Node>>> provisioningFutures = IntStream.range(0, provisionTarget)
                .mapToObj(i -> provisioningExecutorService.<Optional<Node>>submit(() -> {
                    LOG.debug("Provisioning node {}/{} on {}", i + 1, provisionTarget, nodeFactory);
                    //TODO Implement retry behaviour for provisioning failure
                    try {
                        return Optional.of(nodeFactory.provisionNode(tag, baseDir, dataBaseDir));
                    } catch (Exception e) {
                        LOG.warn("Failed to provision node {}/{}", i + 1, provisionTarget, e);
                        return Optional.empty();
                    }
                }))
                .collect(toList());

        // Preserve blocking behaviour by joining to all futures, and report overall success/failure
        ListenableFuture<List<Optional<Node>>> provisioningFuture = Futures.allAsList(provisioningFutures);
        Futures.addCallback(provisioningFuture, new FutureCallback<List<Optional<Node>>>() {
            @Override
            public void onSuccess(@Nullable List<Optional<Node>> result) {
                List<Node> createdNodes = result.stream().filter(Optional::isPresent).map(Optional::get).collect(toList());
                LOG.info("Successfully provisioned {} nodes ({} failed)", createdNodes.size(), result.size() - createdNodes.size());
            }

            @Override
            public void onFailure(Throwable t) {
                // This shouldn't happen, as errors inside the provisioning futures are caught and mapped to Optional.empty()...
                LOG.error("Failed provisioning {} nodes:", provisionTarget, t);
            }
        }, destroyingAndCallbackExecutorService);
        return provisioningFuture;
    }

    @Synchronized
    public void destroyNodes(int destroyTarget, String tag, Path baseDir, long minimumHourFractionUptimeSeconds) {
        // Find up to `destroyTarget` Nodes which have been up for a given proportion of an hour, to maximise utilisation
        long currentEpochSecond = Instant.now().getEpochSecond();
        List<ListenableFuture<Void>> destroyFutures = nodeFactory.getCurrentNodes(tag).stream()
                .filter(node -> (nodeJobs.get(node).isEmpty() && ((currentEpochSecond - node.getCreationEpochSecond()) % 3600 > minimumHourFractionUptimeSeconds)))
                .limit(destroyTarget)
                .map(node -> destroyingAndCallbackExecutorService.<Void>submit(() -> {
                    nodeFactory.destroyNode(node);
                    return null;
                }))
                .collect(toList());

        Futures.addCallback(Futures.allAsList(destroyFutures), new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                LOG.info("Requested destroy of {} nodes", result.size());
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Failed destroying {} nodes:", destroyTarget, t);
            }
        }, destroyingAndCallbackExecutorService);
    }

    @Synchronized
    public void reserveNodeForJob(String jobId) {
        findAvailableNode().ifPresent(node -> {
            LOG.debug("Adding job {} to node {}", jobId, node);
            nodeJobs.put(node, jobId);
        });
    }

    public boolean hasCapacity() {
        return findAvailableNode().isPresent();
    }

    public Node getJobNode(String jobId) {
        return findJobNode(jobId)
                .orElseThrow(() -> new IllegalStateException("Could not find a node for job " + jobId));
    }

    public Optional<Node> findJobNode(String jobId) {
        return nodeJobs.entries().stream()
                .filter(e -> e.getValue().equals(jobId)).findFirst()
                .map(Map.Entry::getKey);
    }

    public void reattachJobToNode(Node node, String jobId) {
        LOG.debug("Reattaching job {} to node {}", jobId,  node);
        nodeJobs.put(node, jobId);
    }

    public void allocateStorageForJob(String jobId, int requiredStorage, String mountPoint) throws StorageProvisioningException {
        Optional<Node> jobNode = Optional.ofNullable(getJobNode(jobId));
        if (jobNode.isPresent()) {
            String volumeId = nodeFactory.allocateStorageForNode(jobNode.get(), requiredStorage, mountPoint);
            jobVolumes.put(jobId, volumeId);
        }
    }

    public void releaseStorageForJob(String jobId) throws StorageProvisioningException {
        if (jobVolumes.containsKey(jobId)) {
            try {
                Node jobNode = findJobNode(jobId).orElseThrow(() -> new StorageProvisioningException("Could not find job node for " + jobId));
                Set<String> volumes = jobVolumes.get(jobId);
                LOG.info("Removing volumes {} for job {}", volumes, jobId);
                nodeFactory.removeStorageForNode(jobNode, volumes);
            } finally {
                jobVolumes.removeAll(jobId);
            }
        }
    }

    public void releaseJobNode(String jobId) {
        Node jobNode = getJobNode(jobId);
        LOG.debug("Releasing node {} for job {}", jobNode.getId(), jobId);
        nodeJobs.get(jobNode).remove(jobId);
        if (jobNode.getTag().equals(DEDICATED_WORKER_TAG)) {
            nodeFactory.destroyNode(jobNode);
        }
    }

    private Optional<Node> findAvailableNode() {
        nodeFactory.syncNodes();
        LOG.debug("Finding available node");
        LOG.debug("POOLED workers: {}", nodeFactory.getCurrentNodes(POOLED_WORKER_TAG));
        LOG.debug("DEDICATED workers: {}", nodeFactory.getCurrentNodes(DEDICATED_WORKER_TAG));

        return nodeFactory.getCurrentNodes(POOLED_WORKER_TAG).stream()
                .filter(node -> {
                    LOG.debug("Found {} jobs running on node {}", nodeJobs.get(node).size(), node);
                    return nodeJobs.get(node).size() < maxJobsPerNode;
                })
                .findFirst();
    }

    private ListenableFuture<Node> provisionNodeAsync(String tag, Path environmentBaseDir) {
        return provisioningExecutorService.submit(() -> nodeFactory.provisionNode(tag, environmentBaseDir, dataBaseDir));
    }

}
