package com.cgi.eoss.ftep.clouds.ipt;

import com.cgi.eoss.ftep.clouds.service.Node;
import com.cgi.eoss.ftep.clouds.service.NodeFactory;
import com.cgi.eoss.ftep.clouds.service.NodePoolStatus;
import com.cgi.eoss.ftep.clouds.service.NodeProvisioningException;
import com.cgi.eoss.ftep.clouds.service.SSHSession;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.ServerUpdateOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.TWO_MINUTES;

/**
 * <p>This service may be used to provision and tear down F-TEP compute nodes in the IPT cloud context.</p>
 */
@Log4j2
public class IptNodeFactory implements NodeFactory {

    private static final int DEFAULT_DOCKER_PORT = 2375;
    private static final String SERVER_NAME_PREFIX = "ftep_node_";
    private static final int SERVER_STARTUP_TIMEOUT_MILLIS = Math.toIntExact(Duration.ofMinutes(10).toMillis());

    @Getter
    private final Set<Node> currentNodes = new HashSet<>();

    private final int maxPoolSize;

    private final IOSClientBuilder.V3 osClientBuilder;

    private final ProvisioningConfig provisioningConfig;

    IptNodeFactory(int maxPoolSize, IOSClientBuilder.V3 osClientBuilder, ProvisioningConfig provisioningConfig) {
        this.maxPoolSize = maxPoolSize;
        this.osClientBuilder = osClientBuilder;
        this.provisioningConfig = provisioningConfig;
    }

    @Override
    public Node provisionNode(Path environmentBaseDir) {
        OSClientV3 osClient = osClientBuilder.authenticate();
        // TODO Check against maxPoolSize
        return provisionNode(osClient, environmentBaseDir, provisioningConfig.getDefaultNodeFlavor());
    }

    // TODO Expose this overload for workers to provision service-specific flavours
    private Node provisionNode(OSClientV3 osClient, Path environmentBaseDir, String flavorName) {
        LOG.info("Provisioning IPT node with flavor '{}'", flavorName);
        Server server = null;
        FloatingIP floatingIp = null;
        try {
            // Generate a random keypair for provisioning
            String keypairName = UUID.randomUUID().toString();
            Keypair keypair = osClient.compute().keypairs().create(keypairName, null);

            Flavor flavor = osClient.compute().flavors().list().stream()
                    .filter(f -> f.getName().equals(flavorName))
                    .findFirst().orElseThrow(() -> new NodeProvisioningException("Could not find flavor: " + flavorName));

            ServerCreate sc = Builders.server()
                    .name(SERVER_NAME_PREFIX + UUID.randomUUID().toString())
                    .flavor(flavor)
                    .image(provisioningConfig.getNodeImageId())
                    .keypairName(keypairName)
                    .addSecurityGroup(provisioningConfig.getSecurityGroupName())
                    .build();

            LOG.info("Provisioning IPT image '{}' to server '{}'", provisioningConfig.getNodeImageId(), sc.getName());
            server = osClient.compute().servers().bootAndWaitActive(sc, SERVER_STARTUP_TIMEOUT_MILLIS);

            floatingIp = getFloatingIp(osClient);
            osClient.compute().floatingIps().addFloatingIP(server, floatingIp.getFloatingIpAddress());
            LOG.info("Allocated floating IP to server: {} to {}", floatingIp.getFloatingIpAddress(), server.getId());
            server = osClient.compute().servers().update(server.getId(), ServerUpdateOptions.create().accessIPv4(floatingIp.getFloatingIpAddress()));

            try (SSHSession ssh = openSshSession(keypair, server)) {
                prepareServer(ssh, environmentBaseDir);
            }

            Node node = Node.builder()
                    .id(server.getId())
                    .name(server.getName())
                    .dockerEngineUrl("tcp://" + server.getAccessIPv4() + ":" + DEFAULT_DOCKER_PORT)
                    .build();
            currentNodes.add(node);
            return node;
        } catch (Exception e) {
            if (server != null) {
                LOG.warn("Tearing down partially-created node {}", server.getId());
                ActionResponse response = osClient.compute().servers().delete(server.getId());
                if (!response.isSuccess()) {
                    LOG.warn("Failed to destroy partially-created node {}", server.getId());
                }
            }
            if (floatingIp != null) {
                osClient.compute().floatingIps().deallocateIP(floatingIp.getId());
            }
            throw new NodeProvisioningException(e);
        }
    }

    private FloatingIP getFloatingIp(OSClientV3 osClient) {
        return getUnallocatedFloatingIp(osClient).orElseGet(() -> getNewFloatingIp(osClient));
    }

    private Optional<FloatingIP> getUnallocatedFloatingIp(OSClientV3 osClient) {
        return osClient.compute().floatingIps().list().stream()
                .filter(ip -> Strings.isNullOrEmpty(ip.getInstanceId()))
                .map(ip -> (FloatingIP) ip)
                .findFirst();
    }

    private FloatingIP getNewFloatingIp(OSClientV3 osClient) {
        FloatingIP floatingIP = osClient.compute().floatingIps().allocateIP(provisioningConfig.getFloatingIpPool());
        LOG.debug("Allocated new floating IP: {}", floatingIP);
        return floatingIP;
    }

    private void prepareServer(SSHSession ssh, Path environmentBaseDir) throws IOException {
        try {
            LOG.debug("IPT node reports hostname: {}", ssh.exec("hostname").getOutput());

            String baseDir = environmentBaseDir.toString();
            LOG.info("Mounting job environment base directory: {}", baseDir);
            ssh.exec("sudo mkdir -p " + baseDir);
            ssh.exec("sudo mount -t nfs " + provisioningConfig.getNfsHost() + ":" + baseDir + " " + baseDir);

            // TODO Use/create a certificate authority for secure docker communication
            LOG.info("Launching dockerd listening on tcp://0.0.0.0:{}", DEFAULT_DOCKER_PORT);
            with().pollInterval(FIVE_HUNDRED_MILLISECONDS)
                    .and().atMost(FIVE_SECONDS)
                    .await("Successfully launched Dockerd")
                    .until(() -> {
                        try {
                            ssh.exec("echo '{\"hosts\":[\"tcp://0.0.0.0:" + DEFAULT_DOCKER_PORT + "\"]}' | sudo tee /etc/docker/daemon.json");
                            ssh.exec("sudo systemctl start docker.service");
                            return ssh.exec("sudo systemctl status docker.service | grep 'API listen on \\[::\\]:2375'").getExitStatus() == 0;
                        } catch (Exception e) {
                            return false;
                        }
                    });
        } catch (Exception e) {
            LOG.error("Failed to prepare server", e);
            throw e;
        }
    }

    private SSHSession openSshSession(Keypair keypair, Server server) throws IOException {
        // Wait until port 22 is open on the server...
        with().pollInterval(FIVE_HUNDRED_MILLISECONDS)
                .and().atMost(TWO_MINUTES)
                .await("SSH socket open")
                .until(() -> {
                    try (SSHSession ssh = new SSHSession(server.getAccessIPv4(), provisioningConfig.getSshUser(), keypair.getPrivateKey(), keypair.getPublicKey())) {
                        return true;
                    } catch (Exception e) {
                        LOG.debug("SSH connection not available for server {}", server.getId(), e);
                        return false;
                    }
                });
        // ...then make the SSH connection
        return new SSHSession(server.getAccessIPv4(), provisioningConfig.getSshUser(), keypair.getPrivateKey(), keypair.getPublicKey());
    }

    @Override
    public void destroyNode(Node node) {
        OSClientV3 osClient = osClientBuilder.authenticate();

        LOG.info("Destroying IPT node: {} ({})", node.getId(), node.getName());
        Server server = osClient.compute().servers().get(node.getId());
        ActionResponse response = osClient.compute().servers().delete(server.getId());
        if (response.isSuccess()) {
            LOG.info("Destroyed IPT node: {}", node.getId());
            currentNodes.remove(node);
        } else {
            LOG.warn("Failed to destroy IPT node {}: [{}] {}", node.getId(), response.getCode(), response.getFault());
        }
        // Check for floating IP
        Optional<? extends FloatingIP> floatingIP = osClient.compute().floatingIps().list().stream().filter(ip -> ip.getFloatingIpAddress().equals(server.getAccessIPv4())).findFirst();
        floatingIP.ifPresent(ip -> osClient.compute().floatingIps().deallocateIP(ip.getId()));
    }

    @Override
    public NodePoolStatus getNodePoolStatus() {
        return NodePoolStatus.builder()
                .maxPoolSize(maxPoolSize)
                .used(currentNodes.size())
                .build();
    }

}
