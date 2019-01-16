package com.cgi.eoss.ftep.rpc;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.net.InetSocketAddress;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * <p>A {@link NameResolver} backed by a {@link DiscoveryClient}. Locates the gRPC service port via the instance
 * metadata map.</p>
 */
@Log4j2
public class DiscoveryClientNameResolver extends NameResolver {
    private static final String GRPC_PORT_METADATA_KEY = "grpcPort";

    private final DiscoveryClient discoveryClient;
    private final String serviceId;
    private final Attributes params;

    private Listener listener;

    public DiscoveryClientNameResolver(DiscoveryClient discoveryClient, String serviceId, Attributes params) {
        this.discoveryClient = discoveryClient;
        this.serviceId = serviceId;
        this.params = params;
    }

    @Override
    public String getServiceAuthority() {
        return serviceId;
    }

    @Override
    public void start(Listener listener) {
        this.listener = listener;
        refresh();
    }

    @Override
    public void refresh() {
        // Convert all ServiceInstances into gRPC's address type
        List<EquivalentAddressGroup> servers = discoveryClient.getInstances(serviceId).stream()
                .map(serviceInstance -> new InetSocketAddress(serviceInstance.getHost(), getGrpcPort(serviceInstance)))
                .map(EquivalentAddressGroup::new) // TODO
                .collect(toList());
        // TODO Extract service config attributes?
        this.listener.onAddresses(servers, Attributes.EMPTY);
    }

    @Override
    public void shutdown() {
    }

    private int getGrpcPort(ServiceInstance serviceInstance) {
        return Integer.parseInt(serviceInstance.getMetadata().getOrDefault(GRPC_PORT_METADATA_KEY, Integer.toString(serviceInstance.getPort())));
    }

}
