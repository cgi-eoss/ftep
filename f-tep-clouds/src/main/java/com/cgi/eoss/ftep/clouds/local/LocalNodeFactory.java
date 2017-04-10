package com.cgi.eoss.ftep.clouds.local;

import com.cgi.eoss.ftep.clouds.service.Node;
import com.cgi.eoss.ftep.clouds.service.NodeFactory;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * <p></p>
 */
@Log4j2
public class LocalNodeFactory implements NodeFactory {

    @Getter
    private final Set<Node> currentNodes = new HashSet<>();

    private final String dockerHostUrl;

    public LocalNodeFactory(String dockerHostUrl) {
        this.dockerHostUrl = dockerHostUrl;
    }

    @Override
    public Node provisionNode() {
        LOG.info("Provisioning LOCAL node");
        Node node = Node.builder()
                .id(UUID.randomUUID().toString())
                .name("LOCAL node")
                .dockerEngineUrl(dockerHostUrl)
                .build();
        currentNodes.add(node);
        return node;
    }

    @Override
    public void destroyNode(Node node) {
        LOG.info("Destroying LOCAL node: {} ({})", node.getId(), node.getName());
        currentNodes.remove(node);
    }

}
