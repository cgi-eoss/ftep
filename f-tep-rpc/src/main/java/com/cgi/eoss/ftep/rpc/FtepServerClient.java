package com.cgi.eoss.ftep.rpc;

import com.cgi.eoss.ftep.rpc.catalogue.CatalogueServiceGrpc;
import com.google.common.collect.Iterables;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Optional;
import java.util.function.Supplier;

public class FtepServerClient {
    private final DiscoveryClient discoveryClient;
    private final String ftepServerServiceId;
    private final Supplier<ManagedChannel> channel =
            new Supplier<ManagedChannel>() {
                ManagedChannel value;

                @Override
                public ManagedChannel get() {
                    return Optional.ofNullable(value).orElseGet(() -> {
                        ServiceInstance ftepServer = Iterables.getOnlyElement(discoveryClient.getInstances(ftepServerServiceId));

                        return ManagedChannelBuilder.forAddress(ftepServer.getHost(), Integer.parseInt(ftepServer.getMetadata().get("grpcPort")))
                                .usePlaintext(true)
                                .build();
                    });
                }
            };

    public FtepServerClient(DiscoveryClient discoveryClient, String ftepServerServiceId) {
        this.discoveryClient = discoveryClient;
        this.ftepServerServiceId = ftepServerServiceId;
    }

    public ServiceContextFilesServiceGrpc.ServiceContextFilesServiceBlockingStub serviceContextFilesServiceBlockingStub() {
        return ServiceContextFilesServiceGrpc.newBlockingStub(channel.get());
    }

    public CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsServiceBlockingStub() {
        return CredentialsServiceGrpc.newBlockingStub(channel.get());
    }

    public CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueServiceBlockingStub() {
        return CatalogueServiceGrpc.newBlockingStub(channel.get());
    }

    public CatalogueServiceGrpc.CatalogueServiceStub catalogueServiceStub() {
        return CatalogueServiceGrpc.newStub(channel.get());
    }

}
