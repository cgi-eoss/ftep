package com.cgi.eoss.ftep.core.wpswrapper;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.zoo.project.ZooConstants;

import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import com.cgi.eoss.ftep.core.requesthandler.ZooConfigHandler;
import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;
import com.cgi.eoss.ftep.core.requesthandler.utils.FtepConstants;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.WaitContainerResultCallback;

public class Sentinel2NdviWorkflow extends AbstractWrapperProc {

  public Sentinel2NdviWorkflow(String dockerImgName) {
    super(dockerImgName);
  }

  private static final Logger LOG = Logger.getLogger(Sentinel2NdviWorkflow.class);
  private static final String DOCKER_IMAGE_NAME = "s2-ndvi";

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static int sentinel2ndvi(HashMap conf, HashMap inputs, HashMap outputs) {

    Sentinel2NdviWorkflow ndviWpsProcessor = new Sentinel2NdviWorkflow(DOCKER_IMAGE_NAME);
    RequestHandler requestHandler = new RequestHandler(conf, inputs, outputs);

    String userid = requestHandler.getUserId();

    int estimatedExecutionCost = requestHandler.estimateExecutionCost();
    // boolean simulateWPS =
    // requestHandler.getInputParamValue(FtepConstants.WPS_SIMULATE,
    // Boolean.class);

    // if (simulateWPS) {
    // // write estimatedCost to output
    // return 3;
    // }

    // account balance (TEP coins)
    if (ndviWpsProcessor.isSufficientCoinsAvailable()) {
      // step 1: create a Job with unique JobID and working directory
      FtepJob job = requestHandler.createJob();

      // step 2: retrieve input data and place it in job's working
      // directory
      List<String> inputFileNames = requestHandler.fetchInputData(job);

      // step 3: get VM worker

      // step 4: start the docker container
      String dkrImage = DOCKER_IMAGE_NAME;
      String dirToMount = job.getWorkingDir().getAbsolutePath();
      String mountPoint = FtepConstants.DOCKER_SENTINEL_WORKFLOWFILE_JOB_MOUNTPOINT;

      Volume volume1 = new Volume(mountPoint);

      DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
          .withDockerHost("tcp://" + requestHandler.getWorkVmIpAddr()).withDockerTlsVerify(true)
          .withDockerCertPath(FtepConstants.DOCKER_CERT_PATH)
          .withApiVersion(FtepConstants.DOCKER_API_VERISON).build();

      DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

      CreateContainerResponse container = dockerClient.createContainerCmd(dkrImage)
          .withVolumes(volume1).withBinds(new Bind(dirToMount, volume1)).exec();


      dockerClient.startContainerCmd(container.getId()).exec();

      String containerID = container.getId();
      int exitCode = dockerClient.waitContainerCmd(containerID)
          .exec(new WaitContainerResultCallback()).awaitStatusCode();

      LOG.info("Execution of docker container with ID " + containerID + " completed with exit code:" + exitCode);

      HashMap output1 = new HashMap();
      output1.put("dataType", "string");
      output1.put("value", job.getOutputDir().list());

      // HashMap out1 = (HashMap) (outputs.get("Result"));
      outputs.put("Result", output1);


    }

    return 3;

  }

}
