package com.cgi.eoss.ftep.clouds.service;

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
     * @return A Node appropriate for the configured implementation.
     */
    Node provisionNode();

    /**
     * <p>Tear down the given node, releasing its resources.</p>
     */
    void destroyNode(Node node);

    /**
     * <p>Return the current set of provisioned nodes managed by this factory.</p>
     */
    Set<Node> getCurrentNodes();

}