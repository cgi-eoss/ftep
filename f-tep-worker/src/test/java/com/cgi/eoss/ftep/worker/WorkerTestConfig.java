package com.cgi.eoss.ftep.worker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.RemoteApiVersion;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 *
 */
@Configuration
public class WorkerTestConfig {

    @Bean
    public DiscoveryClient discoveryClient() {
        return mock(DiscoveryClient.class);
    }

    @Bean
    public InProcessServerBuilder serverBuilder() {
        return InProcessServerBuilder.forName(getClass().getName()).directExecutor();
    }

    @Bean
    public ManagedChannelBuilder channelBuilder() {
        return InProcessChannelBuilder.forName(getClass().getName()).directExecutor();
    }

    @Bean
    @Primary
    public DockerClient dockerClient(@Value("${ftep.worker.docker.hostUrl:unix:///var/run/docker.sock}") String dockerHostUrl) {
        DefaultDockerClientConfig.Builder dockerClientConfigBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(RemoteApiVersion.VERSION_1_24) // TODO later version?
                .withDockerHost(dockerHostUrl);
        DefaultDockerClientConfig dockerClientConfig = dockerClientConfigBuilder.build();
        DockerHttpClient dockerHttpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig.getDockerHost())
                .sslConfig(dockerClientConfig.getSSLConfig())
                .maxConnections(100)
                .build();
        return DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
    }

}
