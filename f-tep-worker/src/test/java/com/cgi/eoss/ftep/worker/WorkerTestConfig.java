package com.cgi.eoss.ftep.worker;

import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 */
@Configuration
public class WorkerTestConfig {

    @Bean
    public InProcessServerBuilder serverBuilder() {
        return InProcessServerBuilder.forName(getClass().getName()).directExecutor();
    }

    @Bean
    public ManagedChannelBuilder channelBuilder() {
        return InProcessChannelBuilder.forName(getClass().getName()).directExecutor();
    }

}
