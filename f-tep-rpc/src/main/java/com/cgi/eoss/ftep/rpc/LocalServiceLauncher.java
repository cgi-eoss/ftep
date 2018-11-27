package com.cgi.eoss.ftep.rpc;

import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.scheduling.annotation.Async;

public class LocalServiceLauncher {
    private final FtepJobLauncherGrpc.FtepJobLauncherStub jobLauncher;

    public LocalServiceLauncher(ManagedChannelBuilder inProcessChannelBuilder) {
        this.jobLauncher = FtepJobLauncherGrpc.newStub(inProcessChannelBuilder.build());
    }

    @Async
    public void asyncSubmitJob(FtepServiceParams serviceParams, StreamObserver<FtepJobResponse> responseObserver) {
        jobLauncher.submitJob(serviceParams, responseObserver);
    }

    @Async
    public void asyncLaunchService(FtepServiceParams serviceParams, StreamObserver<FtepJobResponse> responseObserver) {
        jobLauncher.launchService(serviceParams, responseObserver);
    }

    @Async
    public void asyncCancelJob(CancelJobParams cancelJobParams, StreamObserver<CancelJobResponse> responseObserver) {
        jobLauncher.cancelJob(cancelJobParams, responseObserver);
    }

    @Async
    public void asyncStopJob(StopServiceParams stopServiceParams, StreamObserver<StopServiceResponse> responseObserver) {
        jobLauncher.stopJob(stopServiceParams, responseObserver);
    }

    @Async
    public void asyncBuildService(BuildServiceParams buildServiceParams, StreamObserver<BuildServiceResponse> responseObserver) {
        jobLauncher.buildService(buildServiceParams, responseObserver);
    }
}
