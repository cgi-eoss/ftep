package com.cgi.eoss.ftep.rpc;

import com.cgi.eoss.ftep.rpc.worker.CleanUpResponse;
import com.cgi.eoss.ftep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.ftep.rpc.worker.ExitParams;
import com.cgi.eoss.ftep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.ftep.rpc.worker.JobEnvironment;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
import com.cgi.eoss.ftep.rpc.worker.LaunchContainerResponse;

import io.grpc.ManagedChannelBuilder;

public class LocalWorker {

    private final FtepWorkerGrpc.FtepWorkerBlockingStub worker;

    public LocalWorker(ManagedChannelBuilder inProcessChannelBuilder) {
        this.worker = FtepWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
    }

    public JobEnvironment prepareInputs(JobInputs request) {
        return worker.prepareInputs(request);
    }

    public LaunchContainerResponse launchContainer(JobDockerConfig request) {
        return worker.launchContainer(request);
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
}
