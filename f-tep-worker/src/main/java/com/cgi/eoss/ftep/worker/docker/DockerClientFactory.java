package com.cgi.eoss.ftep.worker.docker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import shadow.dockerjava.com.github.dockerjava.api.DockerClient;

@Component
public class DockerClientFactory {

    private final DockerClient defaultDockerClient;

    @Autowired
    public DockerClientFactory(DockerClient dockerClient) {
        this.defaultDockerClient = dockerClient;
    }

    public DockerClient getDockerClient() {
        // TODO Add more complex docker client determination
        return defaultDockerClient;
    }

}
