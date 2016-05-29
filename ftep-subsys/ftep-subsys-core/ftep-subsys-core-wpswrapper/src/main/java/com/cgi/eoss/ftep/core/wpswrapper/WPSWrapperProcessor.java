package com.cgi.eoss.ftep.core.wpswrapper;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.WaitContainerResultCallback;

public class WPSWrapperProcessor {
	private static final Logger LOG = Logger.getLogger(WPSWrapperProcessor.class);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int startDockerContainer(HashMap conf, HashMap inputs, HashMap outputs) {
		RequestHandler requestHandler = new RequestHandler(conf, inputs, outputs);
		HashMap<String, List<String>> inputItems = requestHandler.getInputItems();

		int estimatedExecutionCost = estimateExecutionCost();
		boolean simulateWPS = getInputParameter("simulateWPS");

		if (simulateWPS) {
			// write estimatedCost to output
			return 3;
		}

		// account balance (TEP coins)
		if (isSufficientCoinsAvailable()) {
			// step 1: create a Job with unique JobID and working directory
			FtepJob job = requestHandler.createJob();

			// step 2: retrieve input data and place it in job's working
			// directory
			requestHandler.fetchInputData(job);

			// step 3: get VM worker

			// step 4: start the docker container
			String dkrImage = "filejoinerimg";
			String dirToMount = job.getWorkingDir().getAbsolutePath();
			String mountPoint = "/workDir/" + job.getWorkingDir().getName();
			String procArg1 = mountPoint + "/f1.txt";
			String procArg2 = mountPoint + "/f2.txt";

			HashMap i3 = (HashMap) (inputs.get("i3"));
			String outputFileName = i3.get("value").toString();
			String procArg3 = mountPoint + "/" + outputFileName;

			HashMap i4 = (HashMap) (inputs.get("i4"));
			String procArg4 = i4.get("value").toString();

			Volume volume1 = new Volume(mountPoint);

			DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
					.withDockerHost("tcp://192.168.3.87:2375").withDockerTlsVerify(false).withApiVersion("1.22")
					.build();
			DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
			CreateContainerResponse container = dockerClient.createContainerCmd(dkrImage).withVolumes(volume1)
					.withBinds(new Bind(dirToMount, volume1)).withCmd(procArg1, procArg2, procArg3, procArg4).exec();

			dockerClient.startContainerCmd(container.getId()).exec();

			int exitCode = dockerClient.waitContainerCmd(container.getId()).exec(new WaitContainerResultCallback())
					.awaitStatusCode();

			HashMap out1 = (HashMap) (outputs.get("out1"));
			out1.put("generated_file", "....." + procArg3);

		}

		return 3;

	}

	private static boolean isSufficientCoinsAvailable() {
		// TODO Auto-generated method stub
		return true;
	}

	private static boolean isValidUser() {
		// TODO Auto-generated method stub
		return true;
	}

	private static int estimateExecutionCost() {
		// TODO Auto-generated method stub
		return 0;
	}

	private static boolean getInputParameter(String string) {
		// TODO Auto-generated method stub
		return false;
	}

}
