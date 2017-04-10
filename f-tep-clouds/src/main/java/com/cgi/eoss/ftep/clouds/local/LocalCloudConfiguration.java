package com.cgi.eoss.ftep.clouds.local;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "ftep.clouds.local.enabled", havingValue = "true", matchIfMissing = true)
public class LocalCloudConfiguration {

    @Value("${ftep.clouds.local.dockerHostUrl:unix:///var/run/docker.sock}")
    private String dockerHostUrl;

    @Bean
    public LocalNodeFactory localNodeFactory() {
        return new LocalNodeFactory(dockerHostUrl);
    }

}
