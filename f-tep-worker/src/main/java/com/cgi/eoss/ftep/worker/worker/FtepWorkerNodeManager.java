package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.clouds.service.Node;
import com.cgi.eoss.ftep.clouds.service.NodeFactory;
import com.cgi.eoss.ftep.clouds.service.NodeProvisioningException;
import com.cgi.eoss.ftep.clouds.service.StorageProvisioningException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class FtepWorkerNodeManager {

    private final int maxJobsPerNode;
    private final int minWorkerNodes;
    private final Path dataBaseDir;
    private final NodeFactory nodeFactory;
    private final JobEnvironmentService jobEnvironmentService;

    // Track which Node is used for each job
    private final SetMultimap<Node, String> nodeJobs = HashMultimap.create();
    // Track dynamic block volumes attached to nodes
    private final SetMultimap<String, String> jobVolumes = HashMultimap.create();

    public static final String POOLED_WORKER_TAG = "pooled-worker-node";
    public static final String DEDICATED_WORKER_TAG = "dedicated-worker-node";

    public FtepWorkerNodeManager(NodeFactory nodeFactory, JobEnvironmentService jobEnvironmentService,
                                 Path dataBaseDir, int maxJobsPerNode, int minWorkerNodes) {
        this.nodeFactory = nodeFactory;
        this.jobEnvironmentService = jobEnvironmentService;
        this.dataBaseDir = dataBaseDir;
        this.maxJobsPerNode = maxJobsPerNode;
        this.minWorkerNodes = minWorkerNodes;
    }

    @PostConstruct
    public void allocateMinNodes() {
        int currentNodes = getCurrentNodes(FtepWorkerNodeManager.POOLED_WORKER_TAG).size();
        if (currentNodes < minWorkerNodes) {
            try {
                provisionNodes(minWorkerNodes - currentNodes, FtepWorkerNodeManager.POOLED_WORKER_TAG, jobEnvironmentService.getBaseDir());
            } catch (NodeProvisioningException e) {
                LOG.error("Failed initial node provisioning: {}", e.getMessage());
            }
        }
    }

    public Node findOrProvisionNodeForJob(String jobId) {
        LOG.info("Finding node for {}", jobId);
        return findJobNode(jobId).orElseGet(() -> {
            LOG.warn("Provisioning new node for {}, this should already have happened!", jobId);
            reserveNodeForJob(jobId);
            return provisionNodeForJob(jobEnvironmentService.getBaseDir(), jobId);
        });
    }

    public boolean hasCapacity() {
        return findAvailableNode() != null;
    }

    @Synchronized
    public void reserveNodeForJob(String jobId) {
        Node availableNode = findAvailableNode();
        if (availableNode != null) {
            LOG.debug("Adding job {} to node {}", jobId, availableNode);
            nodeJobs.put(availableNode, jobId);
        }
    }

    public Node getJobNode(String jobId) {
        return nodeJobs.entries().stream()
                .filter(e -> e.getValue().equals(jobId)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find a node for job " + jobId))
                .getKey();
    }

    public Optional<Node> findJobNode(String jobId) {
        return nodeJobs.entries().stream()
                .filter(e -> e.getValue().equals(jobId)).findFirst()
                .map(Map.Entry::getKey);
    }

    private Node findAvailableNode() {
        LOG.debug("Finding available node");
        LOG.debug("POOLED workers: {}", nodeFactory.getCurrentNodes(POOLED_WORKER_TAG));
        LOG.debug("DEDICATED workers: {}", nodeFactory.getCurrentNodes(DEDICATED_WORKER_TAG));

        for (Node node : nodeFactory.getCurrentNodes(POOLED_WORKER_TAG)) {
            LOG.debug("Found {} jobs running on node {}", nodeJobs.get(node).size(), node);
            if (nodeJobs.get(node).size() < maxJobsPerNode) {
                return node;
            }
        }
        return null;
    }

    public Set<Node> getCurrentNodes(String tag) {
        return nodeFactory.getCurrentNodes(tag);
    }

    public void releaseJobNode(String jobId) {
        Node jobNode = getJobNode(jobId);
        LOG.debug("Releasing node {} for job {}", jobNode.getId(), jobId);
        nodeJobs.get(jobNode).remove(jobId);
        if (jobNode.getTag().equals(DEDICATED_WORKER_TAG)) {
            nodeFactory.destroyNode(jobNode);
        }
    }

    public void provisionNodes(int count, String tag, Path environmentBaseDir) throws NodeProvisioningException {
        for (int i = 0; i < count; i++) {
            LOG.debug("Provisioning node {}/{} on {}", i + 1, count, nodeFactory);
            nodeFactory.provisionNode(tag, environmentBaseDir, dataBaseDir);
        }
    }

    public Node provisionNodeForJob(Path jobDir, String jobId) throws NodeProvisioningException {
        Node node = nodeFactory.provisionNode(DEDICATED_WORKER_TAG, jobDir, dataBaseDir);
        nodeJobs.put(node, jobId);
        return node;
    }

    public void reattachJobToNode(Node node, String jobId) {
        LOG.debug("Reattaching job " + jobId + " to node " + node);
        nodeJobs.put(node, jobId);
    }

    @Synchronized
    public int destroyNodes(int count, String tag, Path environmentBaseDir, long minimumHourFractionUptimeSeconds) {
        Set<Node> freeWorkerNodes = findNFreeWorkerNodes(count, tag, minimumHourFractionUptimeSeconds);
        int destroyableNodes = freeWorkerNodes.size();
        for (Node scaleDownNode : freeWorkerNodes) {
            nodeFactory.destroyNode(scaleDownNode);
        }
        return destroyableNodes;
    }

    private Set<Node> findNFreeWorkerNodes(int n, String tag, long minimumHourFractionUptimeSeconds) {
        Set<Node> freeWorkerNodes = new HashSet<>();
        Set<Node> currentNodes = nodeFactory.getCurrentNodes(tag);
        long currentEpochSecond = Instant.now().getEpochSecond();
        for (Node node : currentNodes) {
            if (nodeJobs.get(node).isEmpty() && ((currentEpochSecond - node.getCreationEpochSecond()) % 3600 > minimumHourFractionUptimeSeconds)) {
                freeWorkerNodes.add(node);
                if (freeWorkerNodes.size() == n) {
                    return freeWorkerNodes;
                }
            }
        }
        return freeWorkerNodes;
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

    public int getNumberOfFreeNodes(String tag) {
        return nodeFactory.getCurrentNodes(tag).stream().filter(n -> nodeJobs.get(n).isEmpty()).collect(Collectors.toSet()).size();
    }

    public Set<Node> getCurrentNodes() {
        return nodeFactory.getCurrentNodes();
    }


}
