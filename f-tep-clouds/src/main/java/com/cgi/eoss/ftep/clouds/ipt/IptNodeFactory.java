package com.cgi.eoss.ftep.clouds.ipt;

import com.cgi.eoss.ftep.clouds.service.Node;
import com.cgi.eoss.ftep.clouds.service.NodeFactory;
import com.cgi.eoss.ftep.clouds.service.NodePoolStatus;
import com.cgi.eoss.ftep.clouds.service.NodeProvisioningException;
import com.cgi.eoss.ftep.clouds.service.SSHSession;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>This service may be used to provision and tear down F-TEP compute nodes in the IPT cloud context.</p>
 */
@Log4j2
public class IptNodeFactory implements NodeFactory {

    private static final int DEFAULT_DOCKER_PORT = 2376;
    private static final String SERVER_NAME_PREFIX = "ftep_node_";
    private static final int SERVER_STARTUP_TIMEOUT_MILLIS = Math.toIntExact(Duration.ofMinutes(10).toMillis());

    @Getter
    private final Set<Node> currentNodes = new HashSet<>();

    private final int maxPoolSize;

    private final OSClientV3 osClient;

    private final String defaultNodeFlavor;

    private final String nodeImageId;

    private final String sshUser;

    IptNodeFactory(int maxPoolSize, OSClientV3 osClient, String defaultNodeFlavor, String nodeImageId, String sshUser) {
        this.maxPoolSize = maxPoolSize;
        this.osClient = osClient;
        this.defaultNodeFlavor = defaultNodeFlavor;
        this.nodeImageId = nodeImageId;
        this.sshUser = sshUser;
    }

    @Override
    public Node provisionNode() {
        // TODO Check against maxPoolSize
        return provisionNode(defaultNodeFlavor);
    }

    // TODO Expose this overload for workers to provision service-specific flavours
    private Node provisionNode(String flavor) {
        LOG.info("Provisioning IPT node with flavor '{}'", flavor);
        try {
            // Generate a random keypair for provisioning
            String keypairName = UUID.randomUUID().toString();
            Keypair keypair = osClient.compute().keypairs().create(keypairName, null);

            ServerCreate sc = Builders.server()
                    .name(SERVER_NAME_PREFIX + UUID.randomUUID().toString())
                    .flavor(flavor)
                    .image(nodeImageId)
                    .keypairName(keypairName)
                    .build();

            LOG.info("Provisioning IPT image '{}' to server '{}'", nodeImageId, sc.getName());
            Server server = osClient.compute().servers().bootAndWaitActive(sc, SERVER_STARTUP_TIMEOUT_MILLIS);

            try (SSHSession ssh = new SSHSession(server.getAccessIPv4(), sshUser, keypair.getPrivateKey(), keypair.getPublicKey())) {
                // TODO Configure the new node
                Session.Command cmd = ssh.exec("hostname");
                LOG.info("IPT node reports hostname: {}", new String(ByteStreams.toByteArray(cmd.getInputStream())));
                cmd.join(5, TimeUnit.SECONDS);
            }

            Node node = Node.builder()
                    .id(server.getId())
                    .name(server.getName())
                    .dockerEngineUrl("tcp://" + server.getAccessIPv4() + ":" + DEFAULT_DOCKER_PORT)
                    .build();
            currentNodes.add(node);
            return node;
        } catch (Exception e) {
            throw new NodeProvisioningException(e);
        }
    }

    @Override
    public void destroyNode(Node node) {
        LOG.info("Destroying IPT node: {} ({})", node.getId(), node.getName());
        ActionResponse response = osClient.compute().servers().delete(node.getId());
        if (response.isSuccess()) {
            LOG.info("Destroyed IPT node: {}", node.getId());
            currentNodes.remove(node);
        } else {
            LOG.warn("Failed to destroy IPT node {}: [{}] {}", node.getId(), response.getCode(), response.getFault());
        }
    }

    @Override
    public NodePoolStatus getNodePoolStatus() {
        return NodePoolStatus.builder()
                .maxPoolSize(maxPoolSize)
                .used(currentNodes.size())
                .build();
    }

}
