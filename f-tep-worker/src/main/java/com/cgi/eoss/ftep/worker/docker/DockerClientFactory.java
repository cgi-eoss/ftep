package com.cgi.eoss.ftep.worker.docker;

import com.cgi.eoss.ftep.worker.DockerRegistryConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.RemoteApiVersion;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.springframework.stereotype.Component;

@Component
public class DockerClientFactory {

    public static DockerClient buildDockerClient(String dockerHostUrl) {
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

    public static DockerClient buildDockerClient(String dockerHostUrl, DockerRegistryConfig dockerRegistryConfig) {
        DefaultDockerClientConfig.Builder dockerClientConfigBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(RemoteApiVersion.VERSION_1_24) // TODO later version?
                .withDockerHost(dockerHostUrl)
                .withRegistryUrl(dockerRegistryConfig.getDockerRegistryUrl())
                .withRegistryUsername(dockerRegistryConfig.getDockerRegistryUsername())
                .withRegistryPassword(dockerRegistryConfig.getDockerRegistryPassword());
        DefaultDockerClientConfig dockerClientConfig = dockerClientConfigBuilder.build();
        DockerHttpClient dockerHttpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig.getDockerHost())
                .sslConfig(dockerClientConfig.getSSLConfig())
                .maxConnections(100)
                .build();
        return DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
    }
}
