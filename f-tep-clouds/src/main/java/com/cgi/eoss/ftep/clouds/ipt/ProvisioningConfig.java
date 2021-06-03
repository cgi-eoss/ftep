package com.cgi.eoss.ftep.clouds.ipt;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
final class ProvisioningConfig {
    private final String defaultNodeFlavor;
    private final String nodeImageId;
    private final String sshUser;
    private final String securityGroupName;
    private final String floatingIpPool;
    private final String networkId;
    private final String nfsHost;
    private final String additionalNfsMounts;
    @Builder.Default
    private String serverNamePrefix = "ftep_node_";
    private boolean provisionFloatingIp;
    private String insecureRegistries;
    private String eodataHost;
    private String eodataDirectory;
    private String eodataMountPoint;
}
