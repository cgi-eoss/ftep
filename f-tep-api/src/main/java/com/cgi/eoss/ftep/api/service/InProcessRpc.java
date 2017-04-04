package com.cgi.eoss.ftep.api.service;

import com.cgi.eoss.ftep.rpc.FtepServiceLauncherGrpc;
import io.grpc.BindableService;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.ServerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;

@Component
public class InProcessRpc {

    private final ManagedChannelBuilder managedChannelBuilder;
    private final ServerImpl server;

    /**
     * <p>Construct a new in-process gRPC wrapper. Note that all gRPC services registered as beans will be injected and
     * loaded in the serverBuilder.</p>
     */
    @Autowired
    public InProcessRpc(List<BindableService> services) throws IOException {
        InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(getClass().getName()).directExecutor();
        services.forEach(serverBuilder::addService);

        this.server = serverBuilder.build();
        this.managedChannelBuilder = InProcessChannelBuilder.forName(getClass().getName()).directExecutor();
    }

    @PostConstruct
    public void startInProcessRpc() throws IOException {
        try {
            this.server.start();
        } catch (Exception e) {
            // Ignore multiple server.start() calls, just in case
            if (!e.getMessage().equals("name already registered: " + getClass().getName())) {
                throw e;
            }
        }
    }

    @PreDestroy
    public void stopInProcessRpc() {
        this.server.shutdownNow();
    }

    public FtepServiceLauncherGrpc.FtepServiceLauncherFutureStub futureFtepServiceLauncher() {
        return FtepServiceLauncherGrpc.newFutureStub(managedChannelBuilder.build());
    }

}
