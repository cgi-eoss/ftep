package com.cgi.eoss.ftep.clouds.ipt;

import com.cgi.eoss.ftep.clouds.ipt.persistence.KeypairRepository;
import com.google.common.collect.ImmutableSet;
import net.schmizz.sshj.SSHClient;
import org.jclouds.ContextBuilder;
import org.jclouds.config.ContextLinking;
import org.jclouds.http.okhttp.config.OkHttpCommandExecutorServiceModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.keystone.config.KeystoneProperties;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.rest.ApiContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Properties;

@Configuration
@Import({IptPersistenceConfiguration.class})
@ConditionalOnProperty(value = "ftep.clouds.ipt.enabled", havingValue = "true")
public class IptCloudConfiguration {

    @Value("${ftep.clouds.ipt.maxPoolSize:-1}")
    private int maxPoolSize;

    @Value("${ftep.clouds.ipt.os.identityEndpoint}")
    private String osIdentityEndpoint;

    @Value("${ftep.clouds.ipt.os.username}")
    private String osUsername;

    @Value("${ftep.clouds.ipt.os.password}")
    private String osPassword;

    @Value("${ftep.clouds.ipt.os.domainName}")
    private String osDomainName;

    @Value("${ftep.clouds.ipt.os.projectName}")
    private String osProjectName;

    @Value("${ftep.clouds.ipt.os.projectId}")
    private String osProjectId;

    @Value("${ftep.clouds.ipt.node.serverNamePrefix:ftep_node_}")
    private String serverNamePrefix;

    @Value("${ftep.clouds.ipt.node.flavorName}")
    private String nodeFlavorName;

    @Value("${ftep.clouds.ipt.node.imageId}")
    private String nodeImageId;

    @Value("${ftep.clouds.ipt.node.provisionFloatingIp}")
    private boolean provisionFloatingIp;

    @Value("${ftep.clouds.ipt.node.floatingIpPool:floatingIpPool}")
    private String floatingIpPool;

    @Value("${ftep.clouds.ipt.node.securityGroupName}")
    private String securityGroupName;

    @Value("${ftep.clouds.ipt.node.sshUsername}")
    private String sshUsername;

    @Value("${ftep.clouds.ipt.node.networkId}")
    private String networkId;

    @Value("${ftep.clouds.ipt.node.nfsHost}")
    private String nfsHost;

    @Value("${ftep.clouds.ipt.node.additionalNfsMounts:#{null}}")
    private String additionalNfsMounts;

    @Value("${ftep.clouds.ipt.node.insecureRegistries:#{null}}")
    private String insecureRegistries;

    @Value("${ftep.clouds.ipt.node.eodataHost}")
    private String eodataHost;

    @Value("${ftep.clouds.ipt.node.eodataDirectory}")
    private String eodataDirectory;

    @Value("${ftep.clouds.ipt.node.eodataMountPoint}")
    private String eodataMountPoint;

    @Autowired
    KeypairRepository keypairRepository;

    @Bean
    public OpenstackAPIs openstackAPIs() {
        String identity = osDomainName + ":" + osUsername; // tenantName:userName
        String credential = osPassword;

        Properties keystoneProperties = new Properties();
        keystoneProperties.put(KeystoneProperties.KEYSTONE_VERSION, "3");
        keystoneProperties.put(KeystoneProperties.SCOPE, "project:" + osProjectName);

        Properties neutronOverrides = new Properties();
        keystoneProperties.forEach(neutronOverrides::put);

        ApiContext<NeutronApi> neutronContext = ContextBuilder.newBuilder("openstack-neutron")
                .endpoint(osIdentityEndpoint)
                .credentials(identity, credential)
                .modules(ImmutableSet.of(new SLF4JLoggingModule(), new OkHttpCommandExecutorServiceModule()))
                .overrides(neutronOverrides)
                .build();

        Properties novaOverrides = new Properties();
        keystoneProperties.forEach(novaOverrides::put);

        ApiContext<NovaApi> novaContext = ContextBuilder.newBuilder("openstack-nova")
                .endpoint(osIdentityEndpoint)
                .credentials(identity, credential)
                .modules(ImmutableSet.of(new SLF4JLoggingModule(), ContextLinking.linkContext(neutronContext), new OkHttpCommandExecutorServiceModule()))
                .overrides(novaOverrides)
                .build();

        return OpenstackAPIs.builder()
                .neutronApi(neutronContext.getApi())
                .novaApi(novaContext.getApi())
                .build();
    }

    @Bean
    public SSHClient sshClient() {
        return new SSHClient();
    }

    @Bean
    public ProvisioningConfig provisioningConfig() {
        return ProvisioningConfig.builder()
                .defaultNodeFlavor(nodeFlavorName)
                .floatingIpPool(floatingIpPool)
                .nodeImageId(nodeImageId)
                .sshUser(sshUsername)
                .securityGroupName(securityGroupName)
                .networkId(networkId)
                .nfsHost(nfsHost)
                .serverNamePrefix(serverNamePrefix)
                .additionalNfsMounts(additionalNfsMounts)
                .provisionFloatingIp(provisionFloatingIp)
                .insecureRegistries(insecureRegistries)
                .eodataHost(eodataHost)
                .eodataDirectory(eodataDirectory)
                .eodataMountPoint(eodataMountPoint)
                .build();
    }

    @Bean
    public IptNodeFactory iptNodeFactory(OpenstackAPIs openstackAPIs) {
        return new IptNodeFactory(maxPoolSize, openstackAPIs, provisioningConfig(), keypairRepository);
    }

}
