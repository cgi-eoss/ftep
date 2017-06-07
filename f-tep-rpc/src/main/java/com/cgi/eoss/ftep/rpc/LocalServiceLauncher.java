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
    public void asyncLaunchService(FtepServiceParams serviceParams, StreamObserver<FtepServiceResponse> responseObserver) {
        FtepServiceLauncherGrpc.FtepServiceLauncherStub serviceLauncher = FtepServiceLauncherGrpc.newStub(inProcessChannelBuilder.build());
        serviceLauncher.launchService(serviceParams, responseObserver);
    }
}
