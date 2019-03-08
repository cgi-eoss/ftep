package com.cgi.eoss.ftep.worker.docker;

import com.cgi.eoss.ftep.worker.DockerRegistryConfig;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.RemoteApiVersion;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import org.springframework.stereotype.Component;

@Component
public class DockerClientFactory {

    public static DockerClient buildDockerClient(String dockerHostUrl) {
        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(RemoteApiVersion.VERSION_1_19)
                .withDockerHost(dockerHostUrl)
                .build();

        DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
                .withMaxTotalConnections(100)
                .withMaxPerRouteConnections(10);

        return DockerClientBuilder.getInstance(dockerClientConfig)
                .withDockerCmdExecFactory(dockerCmdExecFactory)
                .build();
    }

    public static DockerClient buildDockerClient(String dockerHostUrl, DockerRegistryConfig dockerRegistryConfig) {
        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withApiVersion(RemoteApiVersion.VERSION_1_19)
            .withDockerHost(dockerHostUrl)
            .withRegistryUrl(dockerRegistryConfig.getDockerRegistryUrl())
            .withRegistryUsername(dockerRegistryConfig.getDockerRegistryUsername())
            .withRegistryPassword(dockerRegistryConfig.getDockerRegistryPassword())
            .build();

        DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
            .withMaxTotalConnections(100)
            .withMaxPerRouteConnections(10);

        return DockerClientBuilder.getInstance(dockerClientConfig)
            .withDockerCmdExecFactory(dockerCmdExecFactory)
            .build();
    }
}
