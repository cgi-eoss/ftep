package com.cgi.eoss.ftep.core.wpswrapper;

import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult;
import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult.DataDownloadStatus;
import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import com.cgi.eoss.ftep.core.utils.FtepConstants;
import com.cgi.eoss.ftep.core.utils.beans.InsertResult;
import com.cgi.eoss.ftep.core.wpswrapper.utils.LogContainerTestCallback;
import com.cgi.eoss.ftep.model.internal.FtepJob;
import com.cgi.eoss.ftep.model.rest.ResourceJob;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.zoo.project.ZooConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Log4j2
public class LandCoverS2 extends AbstractWrapperProc {

    public LandCoverS2(String dockerImgName) {
        super(dockerImgName);
    }

    private static final String DOCKER_IMAGE_NAME = "ftep-landcovers2";

    // WPS request output variables
    private static final String FINAL_OUTPUT_FILE_VAR = "Result";
    private static final String OUTPUT_MODEL_FILE_VAR = "Model";
    private static final String OUTPUT_CONFUSIONMATRIX_FILE_VAR = "ConfusionMatrix";
    // file patterns
    public static final String RESULT_GEOTIFF_FILE_PATTERN = "*.tif";
    public static final String RESULT_MODEL_FILE_PATTERN = "*.txt";
    public static final String RESULT_CONFUSIONMATRIX_FILE_PATTERN = "*.csv";

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static int LandCoverS2(HashMap conf, HashMap inputs, HashMap outputs) {

        LandCoverS2 processor = new LandCoverS2(DOCKER_IMAGE_NAME);
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
        if (processor.isSufficientCoinsAvailable()) {
            // step 1: create a Job with unique JobID and working directory
            FtepJob job = requestHandler.createJob();
            resourceJob.setJobId(job.getJobId());
            InsertResult insertResult = requestHandler.insertJobRecord(resourceJob);

            resourceJob.setStep(FtepConstants.JOB_STEP_DATA_FETCH);
            requestHandler.updateJobRecord(insertResult, resourceJob);

            // step 2: retrieve input data and place it in job's working
            // directory
            // List<String> inputFileNames = requestHandler.fetchInputData(job);
            DataManagerResult dataManagerResult = requestHandler.fetchInputData(job);

            if (dataManagerResult.getDownloadStatus().equals(DataDownloadStatus.NONE)) {
                LOG.error("Unable to fetch input data");
                return ZooConstants.WPS_SERVICE_FAILED;
            }

            Map<String, List<String>> processInputs = requestHandler.getInputItems();
            String inputsAsJson = requestHandler.toJson(processInputs);
            HashMap<String, String> processOutputs = new HashMap<>();

            resourceJob.setInputs(inputsAsJson);
            resourceJob.setStep(FtepConstants.JOB_STEP_PROC);
            requestHandler.updateJobRecord(insertResult, resourceJob);

            // step 3: get VM worker

            // step 4: start the docker container
            String dkrImage = DOCKER_IMAGE_NAME;
            String dirToMount = job.getWorkingDir().toAbsolutePath().toString();
            String mountPoint = FtepConstants.DOCKER_JOB_MOUNTPOINT;

            String dirToMount2 = job.getWorkingDir().getParent().toString();


            Volume volume1 = new Volume(mountPoint);
            Volume volume2 = new Volume(dirToMount2);

            String workerVmIpAddr = requestHandler.getWorkVmIpAddr();

            DockerClient dockerClient = getDockerClient(workerVmIpAddr);

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

            LOG.info("Processor Logs for job : " + job.getJobId() + "\n" + loggingCallback);

            if (exitCode != 0) {
                LOG.error("Docker Container Execution did not complete successfully");
                return ZooConstants.WPS_SERVICE_FAILED;
            }

            Optional<Path> firstOutputFile = null;
            try (Stream<Path> files = Files.list(job.getOutputDir())) {
                firstOutputFile = files.findFirst();
            } catch (IOException e) {
                LOG.error("Failed to list contents of output directory", e);
            }

            String outputFilename;
            if (firstOutputFile != null && firstOutputFile.isPresent()) {
                outputFilename = firstOutputFile.get().toAbsolutePath().toString();
            } else {
                LOG.error("No output has been produced. Check processor logs for job {}", job.getJobId());
                return ZooConstants.WPS_SERVICE_FAILED;
            }

            LOG.info("Execution of docker container with ID {} completed with exit code: {} and outFileName: {}",
                    containerID, exitCode, outputFilename);

            File outDir = job.getOutputDir().toFile();
            Map<String, String> outputsToCollect = ImmutableMap.of(
                    FINAL_OUTPUT_FILE_VAR, RESULT_GEOTIFF_FILE_PATTERN,
                    OUTPUT_MODEL_FILE_VAR, RESULT_MODEL_FILE_PATTERN,
                    OUTPUT_CONFUSIONMATRIX_FILE_VAR, RESULT_CONFUSIONMATRIX_FILE_PATTERN
            );
            outputsToCollect.forEach((outputName, filePattern) -> {
                String resultFile = requestHandler.getFirstFileMatching(outDir, filePattern);
                HashMap result = (HashMap) (outputs.get(outputName));
                result.put(ZooConstants.ZOO_GENERATED_FILE, resultFile);
                processOutputs.put(outputName, resultFile);
                LOG.info("Output-{} = {}", outputName, resultFile);
            });

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
