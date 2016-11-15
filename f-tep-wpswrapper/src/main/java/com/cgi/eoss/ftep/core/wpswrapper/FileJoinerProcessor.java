package com.cgi.eoss.ftep.core.wpswrapper;

import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult;
import com.cgi.eoss.ftep.core.data.manager.core.DataManagerResult.DataDownloadStatus;
import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import com.cgi.eoss.ftep.core.requesthandler.beans.FtepJob;
import com.cgi.eoss.ftep.core.utils.FtepConstants;
import com.cgi.eoss.ftep.core.utils.beans.InsertResult;
import com.cgi.eoss.ftep.core.utils.rest.resources.ResourceJob;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.zoo.project.ZooConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class FileJoinerProcessor extends AbstractWrapperProc {

    public FileJoinerProcessor(String dockerImgName) {
        super(dockerImgName);
    }

    private static final String DOCKER_IMAGE_NAME = "ftep-file_joiner";

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static int TextFileJoiner(HashMap conf, HashMap inputs, HashMap outputs) {

        FileJoinerProcessor fileJoinerProcessor = new FileJoinerProcessor(DOCKER_IMAGE_NAME);
        RequestHandler requestHandler = new RequestHandler(conf, inputs, outputs);
        ResourceJob resourceJob = new ResourceJob();

        int estimatedExecutionCost = requestHandler.estimateExecutionCost();
        // boolean simulateWPS = requestHandler.getInputParamValue(FtepConstants.WPS_SIMULATE,
        // Boolean.class);
        //
        // if (simulateWPS) {
        // // write estimatedCost to output
        // return 3;
        // }

        // account balance (TEP coins)
        if (fileJoinerProcessor.isSufficientCoinsAvailable()) {
            // step 1: create a Job with unique JobID and working directory

            FtepJob job = requestHandler.createJob();
            InsertResult insertResult = requestHandler.insertJobRecord(resourceJob);

            resourceJob.setStep(FtepConstants.JOB_STEP_DATA_FETCH);
            requestHandler.updateJobRecord(insertResult, resourceJob);
            // step 2: retrieve input data and place it in job's working
            // directory
            DataManagerResult dataManagerResult = requestHandler.fetchInputData(job);
            requestHandler.sleepForSecs(30);

            if (dataManagerResult.getDownloadStatus().equals(DataDownloadStatus.NONE)) {
                LOG.error("Unable to fetch input data");
                return ZooConstants.WPS_SERVICE_FAILED;
            }

            Map<String, List<String>> processInputFiles = dataManagerResult.getUpdatedInputItems();
            List<String> inputFileNames = new ArrayList<>();
            for (List<String> e : processInputFiles.values()) {
                inputFileNames.addAll(e);
            }

            Map<String, List<String>> processInputs = requestHandler.getInputItems();
            String inputsAsJson = requestHandler.toJson(processInputs);
            Map<String, String> processOutputs = new HashMap<>();

            resourceJob.setInputs(inputsAsJson);
            resourceJob.setStep(FtepConstants.JOB_STEP_PROC);
            requestHandler.updateJobRecord(insertResult, resourceJob);
            requestHandler.sleepForSecs(30);

            // step 3: get VM worker

            // step 4: start the docker container
            String dkrImage = DOCKER_IMAGE_NAME;
            String dirToMount = job.getWorkingDir().getParent();
            String jobDirName = job.getWorkingDir().getAbsolutePath();
            ;

            File input1 = new File(inputFileNames.get(0));
            String procArg1 = jobDirName + "/" + FtepConstants.JOB_INPUT_DIR + "/" + input1.getName();

            File input2 = new File(inputFileNames.get(1));
            String procArg2 = jobDirName + "/" + FtepConstants.JOB_INPUT_DIR + "/" + input2.getName();

            HashMap i3 = (HashMap) (inputs.get("i3"));
            String outputFileName = i3.get("value").toString();
            String procArg3 = jobDirName + "/" + FtepConstants.JOB_OUTPUT_DIR + "/" + outputFileName;

            HashMap i4 = (HashMap) (inputs.get("i4"));
            String procArg4 = i4.get("value").toString();

            Volume volume1 = new Volume(dirToMount);
            String workerVmIpAddr = requestHandler.getWorkVmIpAddr();

            DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost("tcp://" + workerVmIpAddr + ":" + FtepConstants.DOCKER_DAEMON_PORT)
                    .withDockerTlsVerify(true).withDockerCertPath("/home/ftep/.docker/")
                    .withApiVersion("1.22").build();
            DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
            CreateContainerResponse container = dockerClient.createContainerCmd(dkrImage)
                    .withVolumes(volume1).withBinds(new Bind(dirToMount, volume1))
                    .withCmd(procArg1, procArg2, procArg3, procArg4).exec();

            LOG.info("Docker  start command arguments: {} {} {} {}", procArg1, procArg2, procArg3, procArg4);
            dockerClient.startContainerCmd(container.getId()).exec();

            int exitCode = dockerClient.waitContainerCmd(container.getId())
                    .exec(new WaitContainerResultCallback()).awaitStatusCode();

            if (exitCode != 0) {
                LOG.error("Docker Container Execution did not complete successfully");
                return ZooConstants.WPS_SERVICE_FAILED;
            }

            String outputFile = job.getOutputDir().getAbsolutePath() + "/" + outputFileName;
            HashMap out1 = (HashMap) (outputs.get("out1"));
            out1.put("generated_file", outputFile);

            processOutputs.put("out1", outputFile);
            String outputsAsJson = requestHandler.toJson(processOutputs);
            resourceJob.setStep(FtepConstants.JOB_STEP_OUTPUT);
            resourceJob.setOutputs(outputsAsJson);
            if (!requestHandler.updateJobRecord(insertResult, resourceJob)) {
                return ZooConstants.WPS_SERVICE_FAILED;
            }
        }
        return ZooConstants.WPS_SERVICE_SUCCEEDED;
    }


}
