package com.cgi.eoss.ftep.orchestrator;

import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * <p>Spring configuration for the F-TEP Orchestrator component.</p>
 * <p>Manages access to distributed workers and provides the {@link FtepServiceLauncher} RPC service.</p>
 */
@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        PersistenceConfig.class
})
@ComponentScan(basePackageClasses = OrchestratorConfig.class)
public class OrchestratorConfig {
    // TODO Use Spring Cloud service discovery for gRPC servers, instead of manual definition + injection

    /**
     * <p>A gRPC channel builder for connecting to the specified remote host for worker services. This channel builder
     * is for {@link WorkerEnvironment#LOCAL} workers.</p>
     */
    @Bean
    public ManagedChannelBuilder localChannelBuilder(@Value("${ftep.orchestrator.worker.local.grpcHost:f-tep-worker}") String host,
                                                     @Value("${ftep.orchestrator.worker.local.grpcPort:6566}") Integer port) {
        return ManagedChannelBuilder.forAddress(host, port);
    }

}
