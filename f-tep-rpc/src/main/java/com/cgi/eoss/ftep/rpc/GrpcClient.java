package com.cgi.eoss.ftep.rpc;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import lombok.extern.log4j.Log4j2;
import org.lognet.springboot.grpc.autoconfigure.GRpcServerProperties;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Log4j2
abstract class GrpcClient {

    private final ReentrantLock channelLock = new ReentrantLock();

    private final GRpcServerProperties gRpcServerProperties;
    private final DiscoveryClientResolverFactory discoveryClientResolverFactory;
    private final String serviceInstanceId;

    // Instantiate a Supplier<ManagedChannel> so that this client can be switched between in-process and standard rpc,
    // and do it lazily so its creation can access gRpcServerProperties
    private final Supplier<Supplier<ManagedChannel>> channelSupplierSupplier = new Supplier<Supplier<ManagedChannel>>() {
        Supplier<ManagedChannel> value;

        @Override
        public Supplier<ManagedChannel> get() {
            return Optional.ofNullable(value).orElseGet(() -> value = buildChannelSupplier());
        }
    };

    private ManagedChannel channel;

    GrpcClient(DiscoveryClientResolverFactory discoveryClientResolverFactory, String serviceInstanceId) {
        this(null, discoveryClientResolverFactory, serviceInstanceId);
    }

    GrpcClient(GRpcServerProperties gRpcServerProperties, DiscoveryClientResolverFactory discoveryClientResolverFactory, String serviceInstanceId) {
        this.gRpcServerProperties = gRpcServerProperties;
        this.discoveryClientResolverFactory = discoveryClientResolverFactory;
        this.serviceInstanceId = serviceInstanceId;
    }

    private Supplier<ManagedChannel> buildChannelSupplier() {
        return Optional.ofNullable(gRpcServerProperties).map(GRpcServerProperties::getInProcessServerName)
                .<Supplier<ManagedChannel>>map(s -> () -> InProcessChannelBuilder.forName(s).build())
                .orElseGet(() -> () -> ManagedChannelBuilder.forTarget(serviceInstanceId)
                        .nameResolverFactory(discoveryClientResolverFactory) // TODO Use java service loading
                        .usePlaintext() // TODO TLS
                        .build());
    }

    ManagedChannel getChannel() {
        // if necessary, update the local reference and return it, otherwise re-establish the channel
        channelLock.lock();
        try {
            return channel = Optional.ofNullable(channel)
                    .filter(c -> !EnumSet.of(ConnectivityState.TRANSIENT_FAILURE, ConnectivityState.SHUTDOWN).contains(c.getState(true))) // if the channel is not in a 'bad' state, return it...
                    .orElseGet(this::establishNewChannel); // ... otherwise create a new channel
        } finally {
            channelLock.unlock();
        }
    }

    private ManagedChannel establishNewChannel() {
        Optional.ofNullable(channel).ifPresent(this::safeShutdownChannel);
        ManagedChannel newChannel = channelSupplierSupplier.get().get();
        LOG.info("Established new channel: {}", newChannel);
        return newChannel;
    }

    private void safeShutdownChannel(ManagedChannel managedChannel) {
        if (!managedChannel.isShutdown()) {
            LOG.info("Shutting down channel: {}", managedChannel);
            managedChannel.shutdown();
        }
    }


}
