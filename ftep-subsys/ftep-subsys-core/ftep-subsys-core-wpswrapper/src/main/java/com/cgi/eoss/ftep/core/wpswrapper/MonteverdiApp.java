package com.cgi.eoss.ftep.core.wpswrapper;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;
import org.zoo.project.ZooConstants;

import com.cgi.eoss.ftep.core.requesthandler.DataManagerResult;
import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;
import com.cgi.eoss.ftep.core.requesthandler.utils.FtepConstants;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.WaitContainerResultCallback;

public class MonteverdiApp extends AbstractWrapperProc {

  public MonteverdiApp(String dockerImgName) {
    super(dockerImgName);
  }

  private static final Logger LOG = Logger.getLogger(MonteverdiApp.class);
  private static final String DOCKER_IMAGE_NAME = "otb";

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static int startAppMonteverdi(HashMap conf, HashMap inputs, HashMap outputs) {

    MonteverdiApp monteverdiApp = new MonteverdiApp(DOCKER_IMAGE_NAME);
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
    if (monteverdiApp.isSufficientCoinsAvailable()) {
      // step 1: create a Job with unique JobID and working directory
      FtepJob job = requestHandler.createJob();

      // step 2: retrieve input data and place it in job's working
      // directory
      // List<String> inputFileNames = requestHandler.fetchInputData(job);
      DataManagerResult dataManagerResult = requestHandler.fetchInputData(job);

      if (dataManagerResult.getDownloadStatus().equals("NONE")) {
        LOG.error("Unable to fetch input data");
        return ZooConstants.WPS_SERVICE_FAILED;
      }

      // step 3: get VM worker

      // step 4: start the docker container
      String dkrImage = DOCKER_IMAGE_NAME;
      String dirToMount = job.getWorkingDir().getAbsolutePath();
      String mountPoint = FtepConstants.DOCKER_JOB_MOUNTPOINT;

      Volume volume1 = new Volume(mountPoint);
      String workerVmIpAddr = requestHandler.getWorkVmIpAddr();


      DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
          .withDockerHost("tcp://" + workerVmIpAddr + ":" + FtepConstants.DOCKER_DAEMON_PORT)
          .withDockerTlsVerify(true).withDockerCertPath(FtepConstants.DOCKER_CERT_PATH)
          .withApiVersion(FtepConstants.DOCKER_API_VERISON).build();

      DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

      String resolution = requestHandler.getInputParamValue("resolution", String.class);
      LOG.info("Application resolution is set to " + resolution);

      String allocatedPort = requestHandler.findFreePortOn(workerVmIpAddr);
      LOG.info("Monteverdi application will start on port: " + allocatedPort);
      ExposedPort tcp5900 = ExposedPort.tcp(FtepConstants.VNC_PORT);
      Ports portBindings = new Ports();
      Binding binding = new Binding(workerVmIpAddr, allocatedPort);
      portBindings.bind(tcp5900, binding);

      CreateContainerResponse container = dockerClient.createContainerCmd(dkrImage)
          .withVolumes(volume1).withBinds(new Bind(dirToMount, volume1)).withExposedPorts(tcp5900)
          .withPortBindings(portBindings).withCmd(resolution).exec();

      String containerID = container.getId();
      dockerClient.startContainerCmd(containerID).exec();

      LogContainerTestCallback loggingCallback = new LogContainerTestCallback(true);
      dockerClient.logContainerCmd(containerID).withStdErr(true).withStdOut(true)
          .withFollowStream(true).withTailAll().exec(loggingCallback);

      int exitCode = dockerClient.waitContainerCmd(containerID)
          .exec(new WaitContainerResultCallback()).awaitStatusCode();

      HashMap result = (HashMap) (outputs.get("Result"));
      result.put("dataType", "string");
      result.put("value", containerID);
    }

    return ZooConstants.WPS_SERVICE_SUCCEEDED;

  }



}
