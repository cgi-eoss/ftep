package com.cgi.eoss.ftep.orchestrator.zoo;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.rpc.WpsServiceDescriptor;
import com.cgi.eoss.ftep.rpc.WpsServiceDescriptors;
import com.cgi.eoss.ftep.rpc.ZooManagerServiceGrpc;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>gRPC client for ZOO management services.</p>
 */
@Component
public class ZooManagerClient {

    private final ManagedChannel channel;

    @Autowired
    public ZooManagerClient(@Qualifier("zooManagerChannelBuilder") ManagedChannelBuilder channelBuilder) {
        // TODO Use service discovery instead of manual definition + injection
        this.channel = channelBuilder.build();
    }

    /**
     * <p>Ask the ZOO Manager component to update the active of WPS services from the given collection.</p>
     */
    public void updateActiveZooServices(List<FtepService> services) {
        ZooManagerServiceGrpc.ZooManagerServiceBlockingStub client = ZooManagerServiceGrpc.newBlockingStub(channel);

        WpsServiceDescriptors.Builder argsBuilder = WpsServiceDescriptors.newBuilder();
        services.stream().map(this::convertToRpcFtepService).forEach(argsBuilder::addServices);

        client.updateActiveZooServices(argsBuilder.build());
    }

    private WpsServiceDescriptor convertToRpcFtepService(FtepService ftepService) {
        return WpsServiceDescriptor.newBuilder()
                .setName(ftepService.getName())
                .setContent(ByteString.copyFromUtf8(ftepService.getServiceDescriptor().toYaml()))
                .build();
    }

}
