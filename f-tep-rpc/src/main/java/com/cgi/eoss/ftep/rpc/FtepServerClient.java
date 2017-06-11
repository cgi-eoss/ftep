package com.cgi.eoss.ftep.rpc;

import com.cgi.eoss.ftep.rpc.catalogue.CatalogueServiceGrpc;
import com.google.common.collect.Iterables;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

public class FtepServerClient {
    private final DiscoveryClient discoveryClient;
    private final String ftepServerServiceId;

    public FtepServerClient(DiscoveryClient discoveryClient, String ftepServerServiceId) {
        this.discoveryClient = discoveryClient;
        this.ftepServerServiceId = ftepServerServiceId;
    }

    public ServiceContextFilesServiceGrpc.ServiceContextFilesServiceBlockingStub serviceContextFilesServiceBlockingStub() {
        return ServiceContextFilesServiceGrpc.newBlockingStub(getChannel());
    }

    public CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsServiceBlockingStub() {
        return CredentialsServiceGrpc.newBlockingStub(getChannel());
    }

    public CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueServiceBlockingStub() {
        return CatalogueServiceGrpc.newBlockingStub(getChannel());
    }

    private ManagedChannel getChannel() {
        ServiceInstance ftepServer = Iterables.getOnlyElement(discoveryClient.getInstances(ftepServerServiceId));

        return ManagedChannelBuilder.forAddress(ftepServer.getHost(), Integer.parseInt(ftepServer.getMetadata().get("grpcPort")))
                .usePlaintext(true)
                .build();
    }

}
