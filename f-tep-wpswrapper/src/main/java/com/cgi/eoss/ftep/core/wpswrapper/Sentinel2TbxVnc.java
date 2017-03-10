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
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import lombok.extern.log4j.Log4j2;
import org.zoo.project.ZooConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log4j2
public class Sentinel2TbxVnc extends AbstractWrapperProc {

    public Sentinel2TbxVnc(String dockerImgName) {
        super(dockerImgName);
    }

    private static final String DOCKER_IMAGE_NAME = "ftep-s2tbx_vnc";

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static int Sentinel2ToolboxV1(HashMap conf, HashMap inputs, HashMap outputs) {

        Sentinel2TbxVnc s2App = new Sentinel2TbxVnc(DOCKER_IMAGE_NAME);
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
        if (s2App.isSufficientCoinsAvailable()) {
            // step 1: create a Job with unique JobID and working directory
            FtepJob job = requestHandler.createJob();
            String jobID = requestHandler.getJobId();
            resourceJob.setJobId(jobID);
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

            String resolution = requestHandler.getInputParamValue("resolution", String.class);

            ExposedPort tcp5900 = ExposedPort.tcp(FtepConstants.VNC_PORT);
            Ports portBindings = new Ports();
            Binding binding = new Binding(workerVmIpAddr, null);
            portBindings.bind(tcp5900, binding);

            int timeoutInHours = FtepConstants.GUI_APPL_TIMEOUT_HOURS;
            String timeout = requestHandler.getInputParamValue("timeout", String.class);
            if (null != timeout) {
                timeoutInHours = Integer.parseInt(timeout);
            }

            DockerClient dockerClient = getDockerClient(workerVmIpAddr);

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
                LOG.info("Found a free port. SNAP Sentinel-2 Toolbox application will start on port: {} for job: {}", hostPort, jobID);
            } else {
                LOG.error("Cannot find a port to start the SNAP Sentinel-2 Toolbox application");
                return ZooConstants.WPS_SERVICE_FAILED;
            }

            LOG.debug("Updating GUI endpoint for SNAP Sentinel-2 Toolbox application job: {}", jobID);
            resourceJob.setGuiEndpoint(hostIp + ":" + hostPort);
            requestHandler.updateJobRecord(insertResult, resourceJob);

            LogContainerTestCallback loggingCallback = new LogContainerTestCallback(true);
            dockerClient.logContainerCmd(containerID).withStdErr(true).withStdOut(true)
                    .withFollowStream(true).withTailAll().exec(loggingCallback);

            int exitCode = dockerClient.waitContainerCmd(containerID)
                    .exec(new WaitContainerResultCallback()).awaitStatusCode(timeoutInHours, TimeUnit.HOURS);

            LOG.info("Application logs for job : " + job.getJobId() + "\n" + loggingCallback);

            if (exitCode != 0) {
                LOG.error("Docker container return with exit code {}", exitCode);
                return ZooConstants.WPS_SERVICE_FAILED;
            }

            HashMap result = (HashMap) (outputs.get("Result"));
            result.put("dataType", "string");
            result.put("value", jobID);

            processOutputs.put("Result", jobID);
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
