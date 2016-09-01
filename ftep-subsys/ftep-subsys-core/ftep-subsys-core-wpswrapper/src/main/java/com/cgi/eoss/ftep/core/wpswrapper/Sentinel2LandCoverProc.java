package com.cgi.eoss.ftep.core.wpswrapper;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.WaitContainerResultCallback;

public class Sentinel2LandCoverProc extends AbstractWrapperProc {

  public Sentinel2LandCoverProc(String dockerImgName) {
    super(dockerImgName);
  }

  private static final Logger LOG = Logger.getLogger(Sentinel2LandCoverProc.class);
  private static final String DOCKER_IMAGE_NAME = "ftep-s2_land_cover";

  // WPS request output variables
  private static final String INTERIM_OUTPUT_FILE_VAR = "InterimResult";
  private static final String MODEL_FILE_VAR = "ModelFile";
  private static final String FINAL_OUTPUT_FILE_VAR = "Result";

  // file patterns
  public static final String RESULT_GEOTIFF_FILE_PATTERN = "*_2*.tif";
  public static final String RESULT_MODEL_FILE_PATTERN = "model.txt";


  @SuppressWarnings({"rawtypes", "unchecked"})
  public static int Sentinel2LandCover(HashMap conf, HashMap inputs, HashMap outputs) {

    Sentinel2LandCoverProc ndviWpsProcessor = new Sentinel2LandCoverProc(DOCKER_IMAGE_NAME);
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
    if (ndviWpsProcessor.isSufficientCoinsAvailable()) {
      // step 1: create a Job with unique JobID and working directory
      FtepJob job = requestHandler.createJob();
      resourceJob.setJobId(job.getJobID());
      InsertResult insertResult = requestHandler.insertJobRecord(resourceJob);

      resourceJob.setStep(FtepConstants.JOB_STEP_DATA_FETCH);
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
      resourceJob.setStep(FtepConstants.JOB_STEP_PROC);
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

      DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
          .withDockerHost("tcp://" + workerVmIpAddr + ":" + FtepConstants.DOCKER_DAEMON_PORT)
          .withDockerTlsVerify(true).withDockerCertPath(FtepConstants.DOCKER_CERT_PATH)
          .withApiVersion(FtepConstants.DOCKER_API_VERISON).build();

      DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

      CreateContainerResponse container =
          dockerClient.createContainerCmd(dkrImage).withVolumes(volume1, volume2)
              .withBinds(new Bind(dirToMount, volume1), new Bind(dirToMount2, volume2)).exec();

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
      LOG.info("Execution of docker container with ID " + containerID + " completed with exit code:"
          + exitCode);

      File outDir = job.getOutputDir();

      String intermediateOutputFile =
          requestHandler.getFirstFileMatching(outDir, FtepConstants.INTERIM_GEOTIFF_FILE_PATTERN);
      HashMap intermediateOutput = (HashMap) (outputs.get(INTERIM_OUTPUT_FILE_VAR));
      intermediateOutput.put(ZooConstants.ZOO_GENERATED_FILE, intermediateOutputFile);
      processOutputs.put(INTERIM_OUTPUT_FILE_VAR, intermediateOutputFile);
      LOG.info("Output-" + INTERIM_OUTPUT_FILE_VAR + " = " + intermediateOutputFile);

      String modelFile = requestHandler.getFirstFileMatching(outDir, RESULT_MODEL_FILE_PATTERN);
      HashMap model = (HashMap) (outputs.get(MODEL_FILE_VAR));
      model.put(ZooConstants.ZOO_GENERATED_FILE, modelFile);
      processOutputs.put(MODEL_FILE_VAR, modelFile);
      LOG.info("Output-" + MODEL_FILE_VAR + " = " + modelFile);

      String resultFile = requestHandler.getFirstFileMatching(outDir, RESULT_GEOTIFF_FILE_PATTERN);
      HashMap result = (HashMap) (outputs.get(FINAL_OUTPUT_FILE_VAR));
      result.put(ZooConstants.ZOO_GENERATED_FILE, resultFile);
      processOutputs.put(FINAL_OUTPUT_FILE_VAR, resultFile);
      LOG.info("Output-" + FINAL_OUTPUT_FILE_VAR + " = " + resultFile);

      resourceJob.setStep(FtepConstants.JOB_STEP_OUTPUT);
      String outputsAsJson = requestHandler.toJson(processOutputs);
      resourceJob.setOutputs(outputsAsJson);
      if (!requestHandler.updateJobRecord(insertResult, resourceJob)) {
        return ZooConstants.WPS_SERVICE_FAILED;
      }
    }
    return ZooConstants.WPS_SERVICE_SUCCEEDED;
  }



}
