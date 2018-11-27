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

    private final ManagedChannelBuilder inProcessChannelBuilder;

    public LocalWorker(ManagedChannelBuilder inProcessChannelBuilder) {
        this.inProcessChannelBuilder = inProcessChannelBuilder;
    }

    public JobEnvironment prepareInputs(JobInputs request) {
        FtepWorkerGrpc.FtepWorkerBlockingStub worker = FtepWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.prepareInputs(request);
    }

    public LaunchContainerResponse launchContainer(JobDockerConfig request) {
        FtepWorkerGrpc.FtepWorkerBlockingStub worker = FtepWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.launchContainer(request);
    }

    public ContainerExitCode waitForContainerExitWithTimeout(ExitWithTimeoutParams request) {
        FtepWorkerGrpc.FtepWorkerBlockingStub worker = FtepWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.waitForContainerExitWithTimeout(request);
    }

    public ContainerExitCode waitForContainerExit(ExitParams request) {
        FtepWorkerGrpc.FtepWorkerBlockingStub worker = FtepWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.waitForContainerExit(request);
    }

    public CleanUpResponse cleanUp(Job request) {
        FtepWorkerGrpc.FtepWorkerBlockingStub worker = FtepWorkerGrpc.newBlockingStub(inProcessChannelBuilder.build());
        return worker.cleanUp(request);
    }
}
