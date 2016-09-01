package com.cgi.eoss.ftep.core.wpswrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.zoo.project.ZooConstants;

import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult;
import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;
import com.cgi.eoss.ftep.core.utils.FtepConstants;
import com.cgi.eoss.ftep.core.utils.beans.InsertResult;
import com.cgi.eoss.ftep.core.utils.rest.resources.ResourceJob;
import com.cgi.eoss.ftep.core.wpswrapper.utils.LogContainerTestCallback;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.WaitContainerResultCallback;

public class MonteverdiAppVnc extends AbstractWrapperProc {

  public MonteverdiAppVnc(String dockerImgName) {
    super(dockerImgName);
  }

  private static final Logger LOG = Logger.getLogger(MonteverdiAppVnc.class);
  private static final String DOCKER_IMAGE_NAME = "ftep-otb_vnc";

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static int MonteverdiAppV1(HashMap conf, HashMap inputs, HashMap outputs) {

    MonteverdiAppVnc monteverdiApp = new MonteverdiAppVnc(DOCKER_IMAGE_NAME);
    RequestHandler requestHandler = new RequestHandler(conf, inputs, outputs);
    ResourceJob resourceJob = new ResourceJob();

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
      resourceJob.setJobId(jobID);
      InsertResult insertResult = requestHandler.insertJobRecord(resourceJob);
      
      resourceJob.setOutputs(FtepConstants.JOB_STEP_DATA_FETCH);
      requestHandler.updateJobRecord(insertResult, resourceJob);
      
      // step 2: retrieve input data and place it in job's working
      // directory
      // List<String> inputFileNames = requestHandler.fetchInputData(job);
      DataManagerResult dataManagerResult = requestHandler.fetchInputData(job);
      
      if (dataManagerResult.getDownloadStatus().equals("NONE")) {
        LOG.error("Unable to fetch input data");
        return ZooConstants.WPS_SERVICE_FAILED;
      }

      Map<String, List<String>> processInputs = dataManagerResult.getUpdatedInputItems();
      String inputsAsJson = requestHandler.toJson(processInputs);
      HashMap<String, String> processOutputs = new HashMap<>();

      resourceJob.setInputs(inputsAsJson);
      resourceJob.setOutputs(FtepConstants.JOB_STEP_PROC);
      requestHandler.updateJobRecord(insertResult, resourceJob);

      // step 3: get VM worker

      // step 4: start the docker container
      String dkrImage = DOCKER_IMAGE_NAME;
      String dirToMount = job.getWorkingDir().getAbsolutePath();
      String mountPoint = FtepConstants.DOCKER_JOB_MOUNTPOINT;
      String dirToMount2 = job.getWorkingDir().getParent();

      Volume volume1 = new Volume(mountPoint);
      Volume volume2 = new Volume(dirToMount2);
      String workerVmIpAddr = requestHandler.getWorkVmIpAddr();

      String resolution = requestHandler.getInputParamValue("resolution", String.class);

      ExposedPort tcp5900 = ExposedPort.tcp(FtepConstants.VNC_PORT);
      Ports portBindings = new Ports();
      Binding binding = new Binding(workerVmIpAddr, null);
      portBindings.bind(tcp5900, binding);

      int timeoutInMins = FtepConstants.GUI_APPL_TIMEOUT_MINUTES;
      String timeout = requestHandler.getInputParamValue("timeout", String.class);
      if (null != timeout) {
        timeoutInMins = Integer.parseInt(timeout);
      }

      DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
          .withDockerHost("tcp://" + workerVmIpAddr + ":" + FtepConstants.DOCKER_DAEMON_PORT)
          .withDockerTlsVerify(true).withDockerCertPath(FtepConstants.DOCKER_CERT_PATH)
          .withApiVersion(FtepConstants.DOCKER_API_VERISON).build();

      DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

      CreateContainerResponse container =
          dockerClient.createContainerCmd(dkrImage).withVolumes(volume1, volume2)
              .withBinds(new Bind(dirToMount, volume1), new Bind(dirToMount2, volume2))
              .withExposedPorts(tcp5900).withPortBindings(portBindings).withCmd(resolution).exec();

      String containerID = container.getId();
      dockerClient.startContainerCmd(containerID).exec();

      InspectContainerResponse inspectContainerResponse =
          dockerClient.inspectContainerCmd(container.getId()).exec();
      Binding portBinding = inspectContainerResponse.getNetworkSettings().getPorts().getBindings()
          .entrySet().iterator().next().getValue()[0];

      String hostIp = portBinding.getHostIp();
      String hostPort = portBinding.getHostPortSpec();

      if (null != hostPort) {
        LOG.info("Found a free port. Monteverdi application will start on port: " + hostPort
            + " for job:" + jobID);
      } else {
        LOG.error("Cannot find a port to start the Monteverdi application");
        return ZooConstants.WPS_SERVICE_FAILED;
      }

      LOG.debug("Updating GUI endpoint for Monteverdi application job:" + jobID);      
      resourceJob.setGuiEndpoint(hostIp + ":" + hostPort);
      requestHandler.updateJobRecord(insertResult, resourceJob);
      
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
      resourceJob.setOutputs(outputsAsJson);
      if (!requestHandler.updateJobRecord(insertResult, resourceJob)) {
        return ZooConstants.WPS_SERVICE_FAILED;
      }
    }

    return ZooConstants.WPS_SERVICE_SUCCEEDED;

  }



}
