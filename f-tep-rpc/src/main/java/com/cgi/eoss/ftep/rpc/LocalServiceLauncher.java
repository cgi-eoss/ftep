package com.cgi.eoss.ftep.rpc;

import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.scheduling.annotation.Async;

public class LocalServiceLauncher {
    private final ManagedChannelBuilder inProcessChannelBuilder;

    public LocalServiceLauncher(ManagedChannelBuilder inProcessChannelBuilder) {
        this.inProcessChannelBuilder = inProcessChannelBuilder;
    }

    @Async
    public void asyncSubmitJob(FtepServiceParams serviceParams, StreamObserver<FtepServiceResponse> responseObserver) {
        FtepJobLauncherGrpc.FtepJobLauncherStub jobLauncher = FtepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.submitJob(serviceParams, responseObserver);
    }

    @Async
    public void asyncLaunchService(FtepServiceParams serviceParams, StreamObserver<FtepServiceResponse> responseObserver) {
        FtepJobLauncherGrpc.FtepJobLauncherStub jobLauncher = FtepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.launchService(serviceParams, responseObserver);
    }

    @Async
    public void asyncStopService(StopServiceParams stopParams, StreamObserver<StopServiceResponse> responseObserver) {
        FtepJobLauncherGrpc.FtepJobLauncherStub jobLauncher = FtepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.stopService(stopParams, responseObserver);
    }

    @Async
    public void asyncCancelJob(CancelJobParams cancelJobParams, StreamObserver<CancelJobResponse> responseObserver) {
        FtepJobLauncherGrpc.FtepJobLauncherStub jobLauncher = FtepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.cancelJob(cancelJobParams, responseObserver);
    }

    @Async
    public void asyncStopJob(StopServiceParams stopServiceParams, StreamObserver<StopServiceResponse> responseObserver) {
        FtepJobLauncherGrpc.FtepJobLauncherStub jobLauncher = FtepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.stopJob(stopServiceParams, responseObserver);
    }

    @Async
    public void asyncBuildService(BuildServiceParams buildServiceParams, StreamObserver<BuildServiceResponse> responseObserver) {
        FtepJobLauncherGrpc.FtepJobLauncherStub jobLauncher = FtepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
        jobLauncher.buildService(buildServiceParams, responseObserver);
    }
}
