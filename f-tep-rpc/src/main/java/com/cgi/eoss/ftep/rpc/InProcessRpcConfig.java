package com.cgi.eoss.ftep.rpc;

import io.grpc.BindableService;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableAsync
public class InProcessRpcConfig {

    private static final String IN_PROCESS_RPC_NAME = UUID.randomUUID().toString();

    @Bean(name = "inProcessChannelBuilder")
    public ManagedChannelBuilder inProcessChannelBuilder() {
        return InProcessChannelBuilder.forName(IN_PROCESS_RPC_NAME).directExecutor();
    }

    @Bean
    public InProcessRpcServer inProcessRpcServer(List<BindableService> services) {
        return new InProcessRpcServer(IN_PROCESS_RPC_NAME, services);
    }

    @Bean
    public LocalServiceLauncher localServiceLauncher(ManagedChannelBuilder inProcessChannelBuilder) {
        return new LocalServiceLauncher(inProcessChannelBuilder);
    }

    private static final class InProcessRpcServer {

        private final Server server;

        /**
         * <p>Construct a new in-process gRPC server. Note that all gRPC services registered as beans will be injected
         * and loaded in the serverBuilder.</p>
         */
        InProcessRpcServer(String name, List<BindableService> services) {
            InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(name).directExecutor();
            services.forEach(serverBuilder::addService);
            this.server = serverBuilder.build();
        }

        @PostConstruct
        public void startInProcessRpc() throws IOException {
            try {
                this.server.start();
            } catch (Exception e) {
                // Ignore multiple server.start() calls, just in case
                if (!e.getMessage().equals("name already registered: " + IN_PROCESS_RPC_NAME)) {
                    throw e;
                }
            }
        }

        @PreDestroy
        public void stopInProcessRpc() {
            this.server.shutdownNow();
        }
    }
}
