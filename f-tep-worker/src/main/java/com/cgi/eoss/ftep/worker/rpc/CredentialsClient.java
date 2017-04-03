package com.cgi.eoss.ftep.worker.rpc;

import com.cgi.eoss.ftep.rpc.CredentialsServiceGrpc;
import com.google.common.collect.Iterables;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Component
public class CredentialsClient {
    private final DiscoveryClient discoveryClient;
    private final String ftepServerServiceId;

    @Autowired
    public CredentialsClient(DiscoveryClient discoveryClient,
                             @Value("${ftep.worker.server.eurekaServiceId:f-tep server}") String ftepServerServiceId) {
        this.discoveryClient = discoveryClient;
        this.ftepServerServiceId = ftepServerServiceId;
    }

    public CredentialsServiceGrpc.CredentialsServiceBlockingStub getBlockingStub() {
        ServiceInstance ftepServer = Iterables.getOnlyElement(discoveryClient.getInstances(ftepServerServiceId));

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(ftepServer.getHost(), Integer.parseInt(ftepServer.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();

        return CredentialsServiceGrpc.newBlockingStub(managedChannel);
    }

}
