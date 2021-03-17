package com.cgi.eoss.ftep.rpc;

import com.cgi.eoss.ftep.rpc.worker.CleanUpResponse;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.DockerImageConfig;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.GetJobEnvironmentRequest;
import com.cgi.eoss.ftep.rpc.worker.GetNodesRequest;
import com.cgi.eoss.ftep.rpc.worker.GetNodesResponse;
import com.cgi.eoss.ftep.rpc.worker.GetResumableJobsRequest;
import com.cgi.eoss.ftep.rpc.worker.GetResumableJobsResponse;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import com.cgi.eoss.ftep.rpc.worker.LaunchContainerResponse;
import com.cgi.eoss.ftep.rpc.worker.PrepareDockerImageResponse;
import com.cgi.eoss.ftep.rpc.worker.TerminateJobRequest;
import io.grpc.ManagedChannelBuilder;

public class LocalWorker {

    private final FtepWorkerGrpc.FtepWorkerBlockingStub worker;

    public LocalWorker(ManagedChannelBuilder inProcessChannelBuilder) {
        this.worker = FtepWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
    }

    public JobEnvironment prepareInputs(JobInputs request) {
        return worker.prepareInputs(request);
    }

    public LaunchContainerResponse launchContainer(JobSpec request) {
        return worker.launchContainer(request);
    }

    public PrepareDockerImageResponse prepareDockerImage(DockerImageConfig dockerImageConfig) {
        return worker.prepareDockerImage(dockerImageConfig);
    }

    public ContainerExitCode waitForContainerExitWithTimeout(ExitWithTimeoutParams request) {
        return worker.waitForContainerExitWithTimeout(request);
    }

    public ContainerExitCode waitForContainerExit(ExitParams request) {
        return worker.waitForContainerExit(request);
    }

    public CleanUpResponse cleanUp(Job request) {
        return worker.cleanUp(request);
    }

    public JobEnvironment getExistingJobEnvironment(GetJobEnvironmentRequest request) {
        return worker.getJobEnvironment(request);
    }

    public GetResumableJobsResponse getResumableJobs(GetResumableJobsRequest request) {
        return worker.getResumableJobs(request);
    }

    public GetNodesResponse getNodes(GetNodesRequest request) {
        return worker.getNodes(request);
    }

    public ContainerExitCode terminateJob(TerminateJobRequest request) {
        return worker.terminateJob(request);
    }
}
