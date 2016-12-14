package com.cgi.eoss.ftep.core.wpswrapper;

import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult;
import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult.DataDownloadStatus;
import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import com.cgi.eoss.ftep.core.utils.FtepConstants;
import com.cgi.eoss.ftep.core.utils.beans.InsertResult;
import com.cgi.eoss.ftep.core.wpswrapper.utils.LogContainerTestCallback;
import com.cgi.eoss.ftep.model.internal.FtepJob;
import com.cgi.eoss.ftep.model.rest.ResourceJob;
import com.cgi.eoss.ftep.orchestrator.ManualWorkerService;
import com.cgi.eoss.ftep.orchestrator.Worker;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class VegetationIndices extends AbstractWrapperProc {

    public VegetationIndices(String dockerImgName) {
        super(dockerImgName);
    }

    private static final String DOCKER_IMAGE_NAME = "ftep-vegetation_indices";

    // WPS request output variables
    private static final String FINAL_OUTPUT_FILE_VAR = "Result";
    // file patterns
    public static final String RESULT_GEOTIFF_FILE_PATTERN = "*.tif";

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static int VegetationIndices(HashMap conf, HashMap inputs, HashMap outputs) {

        VegetationIndices processor = new VegetationIndices(DOCKER_IMAGE_NAME);
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

            Worker worker = new ManualWorkerService().getWorker(workerVmIpAddr);
            DockerClient dockerClient = worker.getDockerClient();

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
            String resultFile = requestHandler.getFirstFileMatching(outDir, RESULT_GEOTIFF_FILE_PATTERN);
            HashMap result = (HashMap) (outputs.get(FINAL_OUTPUT_FILE_VAR));
            result.put(ZooConstants.ZOO_GENERATED_FILE, resultFile);
            processOutputs.put(FINAL_OUTPUT_FILE_VAR, resultFile);
            LOG.info("Output-{} = {}", FINAL_OUTPUT_FILE_VAR, resultFile);

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
