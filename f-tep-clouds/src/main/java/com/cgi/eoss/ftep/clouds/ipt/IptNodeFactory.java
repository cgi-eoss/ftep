package com.cgi.eoss.ftep.clouds.ipt;

import com.cgi.eoss.ftep.clouds.ipt.persistence.KeypairRepository;
import com.cgi.eoss.ftep.clouds.service.Node;
import com.cgi.eoss.ftep.clouds.service.NodeFactory;
import com.cgi.eoss.ftep.clouds.service.NodePoolStatus;
import com.cgi.eoss.ftep.clouds.service.NodeProvisioningException;
import com.cgi.eoss.ftep.clouds.service.SSHSession;
import com.cgi.eoss.ftep.clouds.service.StorageProvisioningException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.log4j.Log4j2;
import org.jclouds.collect.PagedIterable;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.Network;
import org.jclouds.openstack.neutron.v2.features.NetworkApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.KeyPair;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.domain.Volume;
import org.jclouds.openstack.nova.v2_0.domain.Volume.Status;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.openstack.nova.v2_0.options.CreateVolumeOptions;
import shadow.jclouds.com.google.common.collect.Multimap;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.FIVE_MINUTES;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.TWO_SECONDS;

/**
 * <p>This service may be used to provision and tear down FS-TEP compute nodes in the IPT cloud context.</p>
 */
@Log4j2
public class IptNodeFactory implements NodeFactory {

    private static final int DEFAULT_DOCKER_PORT = 2375;
    private static final int SERVER_STARTUP_TIMEOUT_MILLIS = Math.toIntExact(Duration.ofMinutes(10).toMillis());
    private static final org.awaitility.Duration VOLUME_STARTUP_TIMEOUT_DURATION = new org.awaitility.Duration(5, TimeUnit.MINUTES);
    private static final org.awaitility.Duration VOLUME_DETACH_TIMEOUT_DURATION = new org.awaitility.Duration(5, TimeUnit.MINUTES);

    @Getter
    private final Set<Node> currentNodes = ConcurrentHashMap.newKeySet();

    private final Set<String> currentlyUpdatingNodeIds = ConcurrentHashMap.newKeySet();

    private final int maxPoolSize;

    private final ServerApi serverApi;

    private final KeyPairApi keyPairApi;

    private final FloatingIPApi floatingIPApi;

    private final FlavorApi flavorApi;

    private final NetworkApi networkApi;

    private final VolumeApi volumeApi;

    private final VolumeAttachmentApi volumeAttachmentApi;

    private final ProvisioningConfig provisioningConfig;

    private final KeypairRepository keypairRepository;

    IptNodeFactory(int maxPoolSize, OpenstackAPIs openstackAPIs, ProvisioningConfig provisioningConfig, KeypairRepository keypairRepository) {
        this.maxPoolSize = maxPoolSize;

        NovaApi novaApi = openstackAPIs.getNovaApi();
        NeutronApi neutronApi = openstackAPIs.getNeutronApi();
        String region = novaApi.getConfiguredRegions().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not determine OpenStack Nova region"));

        this.serverApi = novaApi.getServerApi(region);
        this.keyPairApi = novaApi.getKeyPairApi(region).get();
        this.floatingIPApi = novaApi.getFloatingIPApi(region).get();
        this.flavorApi = novaApi.getFlavorApi(region);
        this.networkApi = neutronApi.getNetworkApi(region);
        this.volumeApi = novaApi.getVolumeApi(region).get();
        this.volumeAttachmentApi = novaApi.getVolumeAttachmentApi(region).get();
        this.provisioningConfig = provisioningConfig;
        this.keypairRepository = keypairRepository;
    }

    @PostConstruct
    public void init() {
        Set<Node> existingNodes = loadExistingNodes();
        LOG.info("Initialising IptNodeFactory with {} existing nodes", existingNodes.size());
        currentNodes.addAll(existingNodes);
    }

    private Set<Node> loadExistingNodes() {
        PagedIterable<Server> servers = serverApi.listInDetail();
        return StreamSupport.stream(servers.concat().spliterator(), false)
                .filter(server -> server.getName().startsWith(provisioningConfig.getServerNamePrefix())
                        && server.getStatus() == Server.Status.ACTIVE)
                .map(this::createNode)
                .collect(Collectors.toSet());
    }

    @Override
    public void syncNodes() {
        try {
            // Remove any deleted nodes
            Set<String> serverIds = StreamSupport.stream(serverApi.listInDetail().concat().spliterator(), false)
                    .filter(server -> server.getName().startsWith(provisioningConfig.getServerNamePrefix())
                            && server.getStatus() == Server.Status.ACTIVE)
                    .map(Server::getId)
                    .collect(Collectors.toSet());
            currentNodes.removeIf(node -> !serverIds.contains(node.getId()));

            // Add any new nodes
            Set<String> existingNodeIds = currentNodes.stream().map(Node::getId).collect(Collectors.toSet());
            Set<Node> newNodes = StreamSupport.stream(serverApi.listInDetail().concat().spliterator(), false)
                    .filter(server -> server.getName().startsWith(provisioningConfig.getServerNamePrefix())
                            && server.getStatus() == Server.Status.ACTIVE
                            && !currentlyUpdatingNodeIds.contains(server.getId())
                            && !existingNodeIds.contains(server.getId()))
                    .map(this::createNode)
                    .collect(Collectors.toSet());
            currentNodes.addAll(newNodes);
        } catch (Exception e) {
            LOG.debug("Failed to synchronise IPT nodes");
        }
    }

    private Node createNode(Server server) {
        return Node.builder()
                .id(server.getId())
                .name(server.getName())
                .tag(server.getMetadata().get("tag"))
                .creationEpochSecond(server.getCreated().toInstant().getEpochSecond())
                .ipAddress(getServerAddress(server))
                .dockerEngineUrl("tcp://" + getServerAddress(server) + ":" + DEFAULT_DOCKER_PORT)
                .build();
    }

    @Override
    public Node provisionNode(String tag, Path environmentBaseDir, Path dataBaseDir) throws NodeProvisioningException {
        int nodeCount = getCurrentNodes().size();
        if (nodeCount >= maxPoolSize) {
            throw new NodeProvisioningException("Cannot provision node - pool exhausted. Used: " + nodeCount + " Max: " + maxPoolSize);
        }
        return provisionNode(tag, environmentBaseDir, dataBaseDir, provisioningConfig.getDefaultNodeFlavor());
    }

    // TODO Expose this overload for workers to provision service-specific flavours
    private Node provisionNode(String tag, Path environmentBaseDir, Path dataBaseDir, String flavorName) throws NodeProvisioningException {
        LOG.info("Provisioning IPT node with flavor '{}'", flavorName);
        Server server = null;
        FloatingIP floatingIp = null;
        String keypairName = null;
        ServerCreated serverCreated = null;
        try {
            // Generate a random keypair for provisioning
            keypairName = UUID.randomUUID().toString();
            KeyPair keypair = keyPairApi.create(keypairName);
            Flavor flavor = StreamSupport.stream(flavorApi.listInDetail().concat().spliterator(), false)
                    .filter(f -> f.getName().equals(flavorName))
                    .findFirst().orElseThrow(() -> new NodeProvisioningException("Could not find flavor: " + flavorName));

            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("tag", tag);
            CreateServerOptions options = new CreateServerOptions()
                    .metadata(metadata)
                    .keyPairName(keypairName)
                    .networks(provisioningConfig.getNetworkId())
                    .securityGroupNames(provisioningConfig.getSecurityGroupName());

            String newServerName = provisioningConfig.getServerNamePrefix() + UUID.randomUUID().toString();
            LOG.info("Provisioning IPT image '{}' to server '{}'", provisioningConfig.getNodeImageId(), newServerName);
            serverCreated = serverApi.create(newServerName, provisioningConfig.getNodeImageId(), flavor.getId(), options);

            final String serverId = serverCreated.getId();
            currentlyUpdatingNodeIds.add(serverId);
            with().pollInterval(FIVE_SECONDS)
                    .and().atMost(FIVE_MINUTES)
                    .await("Server active")
                    .until(() -> serverApi.get(serverId).getStatus().equals(Server.Status.ACTIVE));

            if (provisioningConfig.isProvisionFloatingIp()) {
                floatingIp = allocateFloatingIp(serverCreated);
            }

            // Refresh network addresses before continuing
            server = serverApi.get(serverCreated.getId());
            String serverIP = getServerAddress(server);
            LOG.info("Server access IP: {}", serverIP);
            try (SSHSession ssh = openSshSession(keypair, server)) {
                prepareServer(ssh, environmentBaseDir, dataBaseDir);
            }

            keypairRepository.save(new com.cgi.eoss.ftep.clouds.ipt.persistence.Keypair(server.getId(), keypair.getPrivateKey(), keypair.getPublicKey()));

            Node node = Node.builder()
                    .id(serverCreated.getId())
                    .name(serverCreated.getName())
                    .tag(tag)
                    .ipAddress(serverIP)
                    .creationEpochSecond(server.getCreated().toInstant().getEpochSecond())
                    .dockerEngineUrl("tcp://" + serverIP + ":" + DEFAULT_DOCKER_PORT)
                    .build();
            currentNodes.add(node);
            return node;
        } catch (Exception e) {
            LOG.error("Error creating node", e);
            if (serverCreated != null) {
                LOG.info("Tearing down partially-created node {}", serverCreated.getId());
                boolean deleted = serverApi.delete(serverCreated.getId());
                currentlyUpdatingNodeIds.remove(serverCreated.getId());
                if (!deleted) {
                    LOG.info("Failed to destroy partially-created node {}", serverCreated.getId());
                }
            }
            if (floatingIp != null) {
                floatingIPApi.delete(floatingIp.getId());
            }
            //Remove the keypair
            keyPairApi.delete(keypairName);
            throw new NodeProvisioningException(e);
        }
    }

    @Synchronized
    private FloatingIP allocateFloatingIp(ServerCreated serverCreated) {
        FloatingIP floatingIp = getFloatingIp();
        floatingIPApi.addToServer(floatingIp.getIp(), serverCreated.getId());
        LOG.info("Allocated floating IP to server: {} to {}", floatingIp.getIp(), serverCreated.getId());
        return floatingIp;
    }

    private FloatingIP getFloatingIp() {
        return getUnallocatedFloatingIp().orElseGet(this::getNewFloatingIp);
    }

    private Optional<FloatingIP> getUnallocatedFloatingIp() {
        return StreamSupport.stream(floatingIPApi.list().spliterator(), false)
                .filter(ip -> Strings.isNullOrEmpty(ip.getInstanceId()))
                .findFirst();
    }

    private FloatingIP getNewFloatingIp() {
        FloatingIP floatingIP = floatingIPApi.allocateFromPool(provisioningConfig.getFloatingIpPool());
        LOG.debug("Allocated new floating IP: {}", floatingIP);
        return floatingIP;
    }

    private String getServerAddress(Server server) {
        Multimap<String, Address> allAddresses = serverApi.get(server.getId()).getAddresses();
        Network network = networkApi.get(provisioningConfig.getNetworkId());
        Collection<Address> networkAddresses = allAddresses.get(network.getName());

        Optional<Address> floatingAddress = networkAddresses.stream().filter(a -> "floating".equals(a.getType().orNull())).findFirst();
        Optional<Address> fixedAddress = networkAddresses.stream().filter(a -> "fixed".equals(a.getType().orNull())).findFirst();

        return Stream.of(floatingAddress, fixedAddress)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Address::getAddr)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Network address not found"));
    }

    private Optional<Address> getServerFloatingIpAddress(Server server) {
        Multimap<String, Address> allAddresses = server.getAddresses();
        Network network = networkApi.get(provisioningConfig.getNetworkId());
        Collection<Address> networkAddresses = allAddresses.get(network.getName());
        return networkAddresses.stream().filter(a -> "floating".equals(a.getType().get())).findFirst();

    }

    private void prepareServer(SSHSession ssh, Path environmentBaseDir, Path dataBaseDir) throws IOException {
        try {
            LOG.debug("IPT node reports hostname: {}", ssh.exec("hostname").getOutput());

            String baseDir = environmentBaseDir.toString();
            String dataBaseDirStr = dataBaseDir.toString();
            LOG.info("Mounting job environment base directory: {}", baseDir);
            ssh.exec("sudo mkdir -p " + baseDir);
            ssh.exec("sudo mkdir -p " + dataBaseDirStr);
            ssh.exec("sudo mount -t nfs " + provisioningConfig.getNfsHost() + ":" + baseDir + " " + baseDir);
            ssh.exec("sudo mount -t nfs " + provisioningConfig.getNfsHost() + ":" + dataBaseDirStr + " " + dataBaseDirStr);
            // mount /eodata share with either NFS or s3fs-fuse
            if (provisioningConfig.isUseS3fsEodata()) {
                // May be enabled by default in newer images, but this will safely enable+mount anyway
                ssh.exec("sudo systemctl enable --now eodata.mount");
            } else {
                ssh.exec("sudo mount -t nfs -o ro " + provisioningConfig.getEodataHost() + ":" + provisioningConfig.getEodataDirectory() + " " + provisioningConfig.getEodataMountPoint());
            }
            String additionalNfsMountsStr = provisioningConfig.getAdditionalNfsMounts();
            if (additionalNfsMountsStr != null) {
                String[] additionalNfsMounts = additionalNfsMountsStr.split(",");
                for (String additionalNfsMount : additionalNfsMounts) {
                    ssh.exec("sudo mkdir -p " + additionalNfsMount);
                    ssh.exec("sudo mount -t nfs " + provisioningConfig.getNfsHost() + ":" + additionalNfsMount + " " + additionalNfsMount);
                }

            }

            if (!Strings.isNullOrEmpty(provisioningConfig.getInsecureRegistries())) {
                String[] insecureRegistriesList = provisioningConfig.getInsecureRegistries().split(",");
                for (String insecureRegistry : insecureRegistriesList) {
                    String host = insecureRegistry.split(":")[0];
                    InetAddress ipAddress = InetAddress.getByName(host);
                    ssh.exec("echo -e \"" + ipAddress.getHostAddress() + "\\t" + host + "\" | sudo tee -a /etc/cloud/templates/hosts.redhat.tmpl");
                    ssh.exec("echo -e \"" + ipAddress.getHostAddress() + "\\t" + host + "\" | sudo tee -a /etc/hosts");
                }
            }

            LOG.debug("Configuring docker");

            String dockerSystemd = "[Service]\nExecStart=\nExecStart=/usr/bin/dockerd -H fd:// --tls=false -H tcp://0.0.0.0:" + DEFAULT_DOCKER_PORT + " --containerd=/run/containerd/containerd.sock";
            ssh.exec("sudo mkdir -p /etc/systemd/system/docker.service.d");
            ssh.exec("sudo echo '" + dockerSystemd + "' | sudo tee /etc/systemd/system/docker.service.d/override.conf");

            StringBuilder dockerConf = new StringBuilder();
            dockerConf.append("{");
            if (!Strings.isNullOrEmpty(provisioningConfig.getInsecureRegistries())) {
                dockerConf.append("\"insecure-registries\":[");
                String[] insecureRegistriesList = provisioningConfig.getInsecureRegistries().split(",");
                String elems = Arrays.stream(insecureRegistriesList).map(s -> "\"" + s + "\"").collect(Collectors.joining(", "));
                dockerConf.append(elems);
                dockerConf.append("]");
            }
            dockerConf.append("}");


            ssh.exec("echo '" + dockerConf.toString() + "' | sudo tee /etc/docker/daemon.json");

            LOG.debug("Restarting docker.service");
            ssh.exec("sudo systemctl daemon-reload && sudo systemctl restart docker.service");

            // TODO Use/create a certificate authority for secure docker communication
            LOG.info("Launching dockerd listening on tcp://0.0.0.0:{}", DEFAULT_DOCKER_PORT);
            with().pollInterval(FIVE_HUNDRED_MILLISECONDS)
                    .and().atMost(FIVE_MINUTES)
                    .await("Successfully launched Dockerd")
                    .until(() -> {
                        try {
                            Preconditions.checkState(ssh.exec(String.format("sudo systemctl status docker.service | grep 'API listen on \\[::\\]:%s'", DEFAULT_DOCKER_PORT)).getExitStatus() == 0);
                            Preconditions.checkState(ssh.exec(String.format("curl -fsL http://127.0.0.1:%s/version >/dev/null", DEFAULT_DOCKER_PORT)).getExitStatus() == 0);
                            return true;
                        } catch (Exception e) {
                            LOG.error("Failed to prepare server", e);
                            return false;
                        }
                    });
        } catch (Exception e) {
            LOG.error("Failed to prepare server", e);
            throw e;
        }
    }

    private SSHSession openSshSession(KeyPair keypair, Server server) throws IOException {
        return openSshSession(keypair.getPrivateKey(), keypair.getPublicKey(), server);
    }

    private SSHSession openSshSession(String privateKey, String publicKey, Server server) throws IOException {
        // Wait until port 22 is open on the server...
        with().pollInterval(TWO_SECONDS)
                .and().atMost(FIVE_MINUTES)
                .await("SSH socket open")
                .until(() -> {
                    try (SSHSession ssh = new SSHSession(getServerAddress(server), provisioningConfig.getSshUser(), privateKey, publicKey)) {
                        return true;
                    } catch (Exception e) {
                        LOG.debug("SSH connection not available for server {}", server.getId(), e);
                        return false;
                    }
                });
        // ...then make the SSH connection
        return new SSHSession(getServerAddress(server), provisioningConfig.getSshUser(), privateKey, publicKey);
    }

    @Override
    public void destroyNode(Node node) {
        LOG.info("Destroying IPT node: {} ({})", node.getId(), node.getName());
        try {
            Server server = serverApi.get(node.getId());
            if (server == null) {
                LOG.warn("Failed to find IPT node by id {}, assuming it was already deleted", node.getId());
                return;
            }

            //Remove the keypair
            try {
                keyPairApi.delete(server.getKeyName());
            } catch (Exception e) {
                LOG.warn("Failed to delete Keypair {} from OpenStack API", server.getKeyName(), e);
            }

            try {
                keypairRepository.deleteById(server.getId());
            } catch (Exception e) {
                LOG.warn("Failed to delete Keypair from local cache", e);
            }

            Optional<Address> floatingIpAddress = getServerFloatingIpAddress(server);
            if (floatingIpAddress.isPresent()) {
                Optional<? extends FloatingIP> floatingIP = StreamSupport.stream(floatingIPApi.list().spliterator(), false)
                        .filter(ip -> ip.getIp().equals(floatingIpAddress.get().getAddr())).findFirst();
                floatingIP.ifPresent(ip -> floatingIPApi.delete(ip.getId()));
            }

            for (VolumeAttachment additionalVolume : volumeAttachmentApi.listAttachmentsOnServer(server.getId())) {
                volumeAttachmentApi.detachVolumeFromServer(additionalVolume.getVolumeId(), additionalVolume.getServerId());
                volumeApi.delete(additionalVolume.getVolumeId());
            }

            boolean deleted = serverApi.delete(server.getId());
            if (deleted) {
                LOG.info("Destroyed IPT node: {}", node.getId());
                currentNodes.remove(node);
            } else {
                LOG.warn("Failed to destroy IPT node {}", node.getId());
            }
        } catch (Exception e) {
            LOG.warn("Failed to destroy IPT node {}", node.getId(), e);
        }
    }

    @Override
    public NodePoolStatus getNodePoolStatus() {
        return NodePoolStatus.builder()
                .maxPoolSize(maxPoolSize)
                .used(currentNodes.size())
                .build();
    }

    @Override
    public Set<Node> getCurrentNodes(String tag) {
        return currentNodes.stream().filter(node -> node.getTag().equals(tag)).collect(Collectors.toSet());
    }

    @Override
    public String allocateStorageForNode(Node node, int storageGB, String mountPoint) throws StorageProvisioningException {
        Server server = serverApi.get(node.getId());
        com.cgi.eoss.ftep.clouds.ipt.persistence.Keypair kp = keypairRepository.findById(server.getId())
                .orElseThrow(() -> new IllegalStateException("No keypair found for server " + server.getId()));
        final Volume volume = createAdditionalVolume(storageGB);
        VolumeAttachment volumeAttachment = volumeAttachmentApi.attachVolumeToServerAsDevice(volume.getId(), server.getId(), "/dev/next");
        with().pollInterval(FIVE_SECONDS)
                .and().atMost(VOLUME_STARTUP_TIMEOUT_DURATION)
                .await("Volume attached")
                .until(() -> volumeApi.get(volume.getId()).getStatus().equals(Status.IN_USE));
        String additionalVolumeDevice = volumeAttachment.getDevice();
        LOG.info("Attached volume to server: {} to {}", volume.getId(), server.getId());
        try {
            SSHSession ssh = openSshSession(kp.getPrivateKey(), kp.getPublicKey(), server);
            ssh.exec("sudo parted -s -a optimal " + additionalVolumeDevice + " mklabel gpt -- mkpart primary ext4 1 -1");
            ssh.exec("sudo mkfs.ext4 " + additionalVolumeDevice + "1");
            ssh.exec("sudo mkdir -p " + mountPoint);
            ssh.exec("sudo mount " + additionalVolumeDevice + "1 " + mountPoint);
            return volume.getId();
        } catch (Exception e) {
            LOG.info("Tearing down partially-created volume {}", volume.getId());
            boolean deleted = volumeApi.delete(volume.getId());
            if (!deleted) {
                LOG.info("Failed to destroy partially-created volume {}", volume.getId());
            }
            throw new StorageProvisioningException("Cannot allocate required storage");
        }
    }

    @Override
    public void removeStorageForNode(Node node, Set<String> volumeIds) throws StorageProvisioningException {
        LOG.debug("Removing volume: {}", volumeIds);
        for (String volumeId : volumeIds) {
            detachVolume(node, volumeId);
        }
    }

    private void detachVolume(Node node, String volumeId) throws StorageProvisioningException {
        Volume volume = volumeApi.get(volumeId);
        try {
            for (VolumeAttachment volumeAttachment : volume.getAttachments()) {
                Server server = serverApi.get(volumeAttachment.getServerId());
                com.cgi.eoss.ftep.clouds.ipt.persistence.Keypair kp = keypairRepository.findById(server.getId())
                        .orElseThrow(() -> new IllegalStateException("No keypair found for server " + server.getId()));
                SSHSession ssh = openSshSession(kp.getPrivateKey(), kp.getPublicKey(), server);
                LOG.debug("Unmounting volume from server: {} to {}", volume.getId(), server.getId());
                ssh.exec("sudo umount " + volumeAttachment.getDevice() + "1 ");
                LOG.debug("Detaching volume from server: {} to {}", volume.getId(), server.getId());
                boolean detached = volumeAttachmentApi.detachVolumeFromServer(volumeAttachment.getVolumeId(), volumeAttachment.getServerId());
                if (detached) {
                    LOG.debug("Detached volume from server: {} to {}", volume.getId(), server.getId());
                } else {
                    LOG.error("Error detaching volume from server: {} to {}", volume.getId(), server.getId());
                }
            }
            LOG.debug("Deleting volume: {}", volume);
            with().pollInterval(FIVE_SECONDS)
                    .and().atMost(VOLUME_DETACH_TIMEOUT_DURATION)
                    .await("Volume available")
                    .until(() -> volumeApi.get(volumeId).getStatus().equals(Status.AVAILABLE));
            boolean deleted = volumeApi.delete(volumeId);
            if (deleted) {
                LOG.info("Deleted volume: {}", volume.getId());
            } else {
                LOG.error("Error deleting volume {}", volume.getId());
            }
        } catch (IOException e) {
            throw new StorageProvisioningException("Cannot remove storage");
        }
    }

    private Volume createAdditionalVolume(int volumeSize) {
        CreateVolumeOptions options = CreateVolumeOptions.Builder
                .volumeType("HDD");

        Volume createdVolume = volumeApi.create(volumeSize, options);
        with().pollInterval(FIVE_SECONDS)
                .and().atMost(VOLUME_STARTUP_TIMEOUT_DURATION)
                .await("Volume available")
                .until(() -> volumeApi.get(createdVolume.getId()).getStatus().equals(Status.AVAILABLE));
        return createdVolume;
    }


}
