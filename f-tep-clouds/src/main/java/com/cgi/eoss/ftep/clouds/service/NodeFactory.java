package com.cgi.eoss.ftep.clouds.service;

import java.nio.file.Path;
import java.util.Set;

/**
 * <p>A service for provisioning F-TEP compute nodes. Implementations of this may work with physical, virtual, or local
 * computing resources, e.g. to instantiate new VMs.</p>
 * <p>Nodes configured by this service should provide the parameters necessary for the F-TEP Worker component to create
 * a Docker client.</p>
 */
public interface NodeFactory {

    /**
     * <p>Provision a Node suitable for running F-TEP services with Docker. This call will block until the requested
     * resource is provisioned.</p>
     *
     * @param environmentBaseDir The base path containing job environments, to be made available to the returned node.
     * @return A Node appropriate for the configured implementation.
     */
    Node provisionNode(Path environmentBaseDir);

    /**
     * <p>Provision a Node suitable for running F-TEP services with Docker.
     * Sets a custom tag and dataBaseDir.</p>
     */
    Node provisionNode(String tag, Path environmentBaseDir, Path dataBaseDir) throws NodeProvisioningException;

    /**
     * <p>Tear down the given node, releasing its resources.</p>
     */
    void destroyNode(Node node);

    /**
     * <p>Return the current set of provisioned nodes managed by this factory.</p>
     */
    Set<Node> getCurrentNodes();

    /**
     * <p>Return the current set of provisioned nodes identified by the given tag and managed by this factory.</p>
     */
    Set<Node> getCurrentNodes(String tag);

    /**
     * <p>Get current available and in-use node statistics.</p>
     */
    NodePoolStatus getNodePoolStatus();

    String allocateStorageForNode(Node node, int storageGB, String mountPoint) throws StorageProvisioningException;
    void removeStorageForNode(Node node, String storageId) throws StorageProvisioningException;
}
