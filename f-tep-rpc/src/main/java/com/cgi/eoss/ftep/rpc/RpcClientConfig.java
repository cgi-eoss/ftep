package com.cgi.eoss.ftep.rpc;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableDiscoveryClient
public class RpcClientConfig {

    @Bean
    public DiscoveryClientResolverFactory discoveryClientResolverFactory(DiscoveryClient discoveryClient) {
        return new DiscoveryClientResolverFactory(discoveryClient);
    }

}
