package com.cgi.eoss.ftep.clouds.ipt;

import net.schmizz.sshj.SSHClient;
import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "ftep.clouds.ipt.enabled", havingValue = "true")
public class IptCloudConfiguration {

    @Value("${ftep.clouds.ipt.maxPoolSize:-1}")
    private int maxPoolSize;

    @Value("${ftep.clouds.ipt.os.identityEndpoint:https://eocloud.eu:5000/v3}")
    private String osIdentityEndpoint;

    @Value("${ftep.clouds.ipt.os.username}")
    private String osUsername;

    @Value("${ftep.clouds.ipt.os.password}")
    private String osPassword;

    @Value("${ftep.clouds.ipt.os.domainName}")
    private String osDomainName;

    @Value("${ftep.clouds.ipt.os.projectWithEoId}")
    private String osProjectWithEoId;

    @Value("${ftep.clouds.ipt.os.projectWithoutEoId}")
    private String osProjectWithoutEoId;

    @Value("${ftep.clouds.ipt.node.flavorName:eo1.large}")
    private String nodeFlavorName;

    @Value("${ftep.clouds.ipt.node.imageId}")
    private String nodeImageId;

    @Value("${ftep.clouds.ipt.node.floatingIpPool:external-network}")
    private String floatingIpPool;

    @Value("${ftep.clouds.ipt.node.securityGroupName:allow_ftep_services}")
    private String securityGroupName;

    @Value("${ftep.clouds.ipt.node.sshUsername:eouser}")
    private String sshUsername;

    @Value("${ftep.clouds.ipt.node.nfsHost}")
    private String nfsHost;

    @Bean
    public IOSClientBuilder.V3 osClientBuilder() {
        return OSFactory.builderV3()
                .withConfig(Config.newConfig()
                        .withConnectionTimeout(60000)
                        .withReadTimeout(60000))
                .endpoint(osIdentityEndpoint)
                .credentials(osUsername, osPassword, Identifier.byName(osDomainName))
                .scopeToProject(Identifier.byId(osProjectWithoutEoId));
    }

    @Bean
    public SSHClient sshClient() {
        return new SSHClient();
    }

    @Bean
    public IptNodeFactory iptNodeFactory(IOSClientBuilder.V3 osClientBuilder) {
        return new IptNodeFactory(maxPoolSize, osClientBuilder,
                ProvisioningConfig.builder()
                        .defaultNodeFlavor(nodeFlavorName)
                        .floatingIpPool(floatingIpPool)
                        .nodeImageId(nodeImageId)
                        .sshUser(sshUsername)
                        .securityGroupName(securityGroupName)
                        .nfsHost(nfsHost)
                        .build()
        );
    }

}
