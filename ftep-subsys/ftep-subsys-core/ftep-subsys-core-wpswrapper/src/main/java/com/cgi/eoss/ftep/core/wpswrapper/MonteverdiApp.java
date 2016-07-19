package com.cgi.eoss.ftep.core.wpswrapper;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.zoo.project.ZooConstants;

import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult;
import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;
import com.cgi.eoss.ftep.core.requesthandler.beans.InsertResult;
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
      String jobID = requestHandler.getJobId();

      // step 2: retrieve input data and place it in job's working
      // directory
      // List<String> inputFileNames = requestHandler.fetchInputData(job);
       DataManagerResult dataManagerResult = requestHandler.fetchInputData(job);
      HashMap<String, List<String>> processInputs = dataManagerResult.getUpdatedInputItems();
      String inputsAsJson = requestHandler.toJson(processInputs);
      HashMap<String, String> processOutputs = new HashMap<>();

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
      LOG.debug("Monteverdi application resolution is set to " + resolution);

      int allocatedPort = requestHandler.findFreePortOn(workerVmIpAddr);
      if (allocatedPort < 0) {
        LOG.error("Cannot find a free port on " + workerVmIpAddr
            + " to start the Monteverdi application");
        return ZooConstants.WPS_SERVICE_FAILED;
      }
      LOG.info("Monteverdi application will start on port: " + allocatedPort + " for job:" + jobID);
      ExposedPort tcp5900 = ExposedPort.tcp(FtepConstants.VNC_PORT);
      Ports portBindings = new Ports();
      Binding binding = new Binding(workerVmIpAddr, Integer.toString(allocatedPort));
      portBindings.bind(tcp5900, binding);

      CreateContainerResponse container = dockerClient.createContainerCmd(dkrImage)
          .withVolumes(volume1).withBinds(new Bind(dirToMount, volume1)).withExposedPorts(tcp5900)
          .withPortBindings(portBindings).withCmd(resolution).exec();

      int timeoutInMins = FtepConstants.GUI_APPL_TIMEOUT_MINUTES;
      String timeout = requestHandler.getInputParamValue("timeout", String.class);
      if (null != timeout) {
        timeoutInMins = Integer.parseInt(timeout);
      }
      LOG.debug("Inserting job record for Monteverdi application job:" + jobID);
      InsertResult resourceEndpoint =
          requestHandler.insertJob(inputsAsJson, "", workerVmIpAddr + ":" + allocatedPort);

      String containerID = container.getId();
      dockerClient.startContainerCmd(containerID).exec();

      LogContainerTestCallback loggingCallback = new LogContainerTestCallback(true);
      dockerClient.logContainerCmd(containerID).withStdErr(true).withStdOut(true)
          .withFollowStream(true).withTailAll().exec(loggingCallback);

      int exitCode = dockerClient.waitContainerCmd(containerID)
          .exec(new WaitContainerResultCallback()).awaitStatusCode(timeoutInMins, TimeUnit.MINUTES);

      LOG.info("Application logs for job : " + job.getJobID() + "\n" + loggingCallback);

      if (exitCode != 0) {
        LOG.error("Docker container return with exit code " + exitCode);
        return ZooConstants.WPS_SERVICE_FAILED;
      }

      HashMap result = (HashMap) (outputs.get("Result"));
      result.put("dataType", "string");
      result.put("value", jobID);

      processOutputs.put("Result", jobID);
      String outputsAsJson = requestHandler.toJson(processOutputs);

      requestHandler.updateJobOutput(resourceEndpoint, outputsAsJson);
    }

    return ZooConstants.WPS_SERVICE_SUCCEEDED;

  }



}
