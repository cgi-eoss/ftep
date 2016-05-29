package com.cgi.eoss.ftep.core.wpswrapper;

import java.util.concurrent.TimeUnit;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.WaitContainerResultCallback;

public class DockerManager {

	public static void main(String[] args) {

		DockerManager dockerManager = new DockerManager();
		dockerManager.start();

	}

	private void start() {

		// DockerClientConfig config =
		// DockerClientConfig.createDefaultConfigBuilder()
		// .withDockerHost("tcp://localhost:2375").withDockerTlsVerify(false).withApiVersion("1.22").build();
		// DockerClient dockerClient =
		// DockerClientBuilder.getInstance(config).build();
		// CreateContainerResponse container =
		// dockerClient.createContainerCmd("busybox").withCmd("sleep",
		// "100").exec();
		// System.out.println("Created container: {} " + container.toString());
		//
		// dockerClient.startContainerCmd(container.getId()).exec();
		// WaitContainerResultCallback callback =
		// dockerClient.waitContainerCmd(container.getId())
		// .exec(new WaitContainerResultCallback());
		// try {
		// callback.awaitStatusCode(100, TimeUnit.MILLISECONDS);
		// System.err.println("Should throw exception on timeout.");
		// } catch (DockerClientException e) {
		// System.out.println(e.getMessage());
		// }
		String dkrImage = "filejoinerimg";
		String dirToMount = "/home/ftep/02-Installations/01-dkr-build/filejoiner/testdata";
		String mountPoint = "/workDir";
		String procArg1 = mountPoint + "/f1.txt";
		String procArg2 = mountPoint + "/f2.txt";
		String procArg3 = mountPoint + "/wpsout.txt";
		String procArg4 = "30";

		Volume volume1 = new Volume(mountPoint);

		DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost("tcp://localhost:2375").withDockerTlsVerify(false).withApiVersion("1.22").build();
		DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
		CreateContainerResponse container = dockerClient.createContainerCmd(dkrImage).withVolumes(volume1)
				.withBinds(new Bind(dirToMount, volume1)).withCmd(procArg1, procArg2, procArg3, procArg4).exec();
		System.out.println("Created container: {}  " + container.toString());

		dockerClient.startContainerCmd(container.getId()).exec();
	}

}
