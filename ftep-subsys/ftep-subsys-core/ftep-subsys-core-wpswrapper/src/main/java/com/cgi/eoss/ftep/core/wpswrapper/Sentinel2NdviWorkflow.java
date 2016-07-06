package com.cgi.eoss.ftep.core.wpswrapper;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.zoo.project.ZooConstants;

import com.cgi.eoss.ftep.core.requesthandler.DataManagerResult;
import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;
import com.cgi.eoss.ftep.core.requesthandler.beans.TableFtepJob;
import com.cgi.eoss.ftep.core.requesthandler.utils.FtepConstants;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.gson.Gson;

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
    TableFtepJob jobRecord = new TableFtepJob();
    Gson gson = new Gson();


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
      // List<String> inputFileNames = requestHandler.fetchInputData(job);
      DataManagerResult dataManagerResult = requestHandler.fetchInputData(job);

      HashMap<String, List<String>> processInputs = dataManagerResult.getUpdatedInputItems();
      String inputsAsJson = gson.toJson(processInputs);

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

      CreateContainerResponse container = dockerClient.createContainerCmd(dkrImage)
          .withVolumes(volume1).withBinds(new Bind(dirToMount, volume1)).exec();

      String containerID = container.getId();
      dockerClient.startContainerCmd(containerID).exec();

      LogContainerTestCallback loggingCallback = new LogContainerTestCallback(true);
      dockerClient.logContainerCmd(containerID).withStdErr(true).withStdOut(true)
          .withFollowStream(true).withTailAll().exec(loggingCallback);

      int exitCode = dockerClient.waitContainerCmd(containerID)
          .exec(new WaitContainerResultCallback()).awaitStatusCode();

      LOG.info("Processor Logs for job : " + job.getJobID() + "\n" + loggingCallback);

      if (exitCode != 0) {
        LOG.error("Docker Container Execution did not complete successfully");
        return ZooConstants.WPS_SERVICE_FAILED;
      }

      String[] outputFiles = job.getOutputDir().list();
      String outputFilename = "";

      if (null != outputFiles && outputFiles.length > 0) {
        outputFilename = new File(job.getOutputDir(), outputFiles[0]).getAbsolutePath();
      } else {
        LOG.error("No output has been produced. Check processor logs for job " + job.getJobID());
        return ZooConstants.WPS_SERVICE_FAILED;
      }

      LOG.info("Execution of docker container with ID " + containerID + " completed with exit code:"
          + exitCode + " outFileName:" + outputFilename);

      HashMap result = (HashMap) (outputs.get("Result"));
      result.put("generated_file", outputFilename);
      processOutputs.put("Result", outputFilename);
      String outputsAsJson = gson.toJson(processOutputs);

      requestHandler.updateJob(inputsAsJson, outputsAsJson, "NA");
    }
    return ZooConstants.WPS_SERVICE_SUCCEEDED;
  }

}
