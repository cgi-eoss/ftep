/*
 * This software is developed by CGI IT UK Ltd.
 * This file may only be reproduced in whole or in part, or stored in a retrieval system,
 * or transmitted in any form, or by any means electronic, mechanical, photocopying
 * or otherwise, either with the prior permission of CGI IT UK Ltd. or in accordance with
 * the terms of a contract made with CGI IT UK Ltd. on use of this source code.
 */
package com.cgi.eoss.ftep.core.wpswrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.WaitContainerResultCallback;

/*
 * @author prithivirajr
 * 3 May 2016
 */
public class DemoWPSProcessor {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int delayOperation(HashMap conf, HashMap inputs, HashMap outputs) {
		HashMap hm1 = new HashMap();
		hm1.put("dataType", "string");
		HashMap tmp = (HashMap) (inputs.get("Mins"));
		java.lang.String v = tmp.get("value").toString();
		HashMap senvMap = (HashMap) (conf.get("senv"));

		String userid = "myString";
		if (null != senvMap && senvMap.size() != 0) {
			userid = (String) senvMap.get("userid");
		} else
			userid = "Senv is NULL";
		hm1.put("value", "Processor will sleep for " + v + " Minutes! " + userid);

		outputs.put("Result", hm1);

		System.err.println("Hello from JAVA WOrld !");
		try {
			TimeUnit.SECONDS.sleep(Integer.parseInt(v));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return 3;
		// return ZOO.SERVICE_SUCCEEDED;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int mapPrinter(HashMap conf, HashMap inputs, HashMap outputs) {

		RequestHandler requestHandler = new RequestHandler(conf, inputs, outputs);
		requestHandler.getInputItems();

		HashMap lenvMap = (HashMap) (conf.get("lenv"));
		long now = new Date().getTime();
		lenvMap.put("cookie", "MMID=MM"+now+"; path=/");

		HashMap senvMap = (HashMap) (conf.get("senv"));
		if (null == senvMap)
			senvMap = new HashMap();

		senvMap.put("MMID", "MM"+now);
		senvMap.put("userid", "rakesh");
		conf.put("senv", senvMap);

		HashMap output1 = new HashMap();
		HashMap output2 = new HashMap();
		HashMap output3 = new HashMap();

		HashMap i1 = (HashMap) (inputs.get("i1"));
		HashMap i2 = (HashMap) (inputs.get("i2"));

		Set entrySet1 = i1.entrySet();
		Set entrySet2 = i2.entrySet();
		Set entrySet3 = conf.entrySet();

		output1.put("dataType", "string");
		output1.put("value", entrySet1.toString());

		output2.put("dataType", "string");
		output2.put("value", entrySet2.toString());

		output3.put("dataType", "string");
		output3.put("value", entrySet3.toString());

		outputs.put("Map1", output1);
		outputs.put("Map2", output2);
		outputs.put("MainConf", output3);
		return 3;

		// return ZOO.SERVICE_SUCCEEDED;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int startContainer(HashMap conf, HashMap inputs, HashMap outputs) {

		HashMap i4 = (HashMap) (inputs.get("i4"));

		String dkrImage = "filejoinerimg";
		String dirToMount = "/home/ftep/02-Installations/01-dkr-build/filejoiner/testdata";
		String mountPoint = "/workDir";
		String procArg1 = mountPoint + "/f1.txt";
		String procArg2 = mountPoint + "/f2.txt";
		String procArg3 = mountPoint + "/wpsout.txt";
		String procArg4 = i4.get("value").toString();

		Volume volume1 = new Volume(mountPoint);

		DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost("tcp://localhost:2375").withDockerTlsVerify(false).withApiVersion("1.22").build();
		DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
		CreateContainerResponse container = dockerClient.createContainerCmd(dkrImage).withVolumes(volume1)
				.withBinds(new Bind(dirToMount, volume1)).withCmd(procArg1, procArg2, procArg3, procArg4).exec();
		// System.out.println("Created container: {} " + container.toString());

		dockerClient.startContainerCmd(container.getId()).exec();
		int exitCode = dockerClient.waitContainerCmd(container.getId()).exec(new WaitContainerResultCallback())
				.awaitStatusCode();
		// if (exitCode > 0) {
		try {
			// System.out.println("Started COPYING FILES with exitCode:" +
			// exitCode);
			File fout = new File(dirToMount, "wpsout.txt");
			File temOut = new File("/tmp/outputfile.txt");
			FileUtils.copyFile(fout, temOut);
			Path path = Paths.get("/tmp/outputfile.txt");
			// List<String> data = Files.readAllLines(path);
			// System.out.println("WRITING bytes to value:");

			HashMap hm1 = (HashMap) (outputs.get("out1"));

			// FIXME
			/*
			 * following lines throws exception and the WPS request fails
			 */
			hm1.put("generated_file", "....." + fout.getAbsolutePath());
			// hm1.put("generated_file", "/tmp/outputfile.txt");
			// hm1.put("value", "myValue");

			// outputs.put("out1", hm1);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		// } else {
		// System.out.println("Container exit code is: " + exitCode);
		// }

		return 3;
	}

}
