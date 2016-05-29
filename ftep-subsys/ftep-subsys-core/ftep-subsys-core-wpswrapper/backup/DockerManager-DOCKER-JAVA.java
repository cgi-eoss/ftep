package com.cgi.eoss.ftep.core.wpswrapper;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

public class DockerManager {

	public static void main(String[] args) {

		DockerManager dockerManager = new DockerManager();
		dockerManager.start();

	}

	private void start() {

		DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost("tcp://localhost:2375").withDockerTlsVerify(false).withApiVersion("1.22").build();
		DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
		CreateContainerResponse container = dockerClient.createContainerCmd("busybox").withCmd("touch", "/test").exec();

		dockerClient.startContainerCmd(container.getId()).exec();
	}

}
