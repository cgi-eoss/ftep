package com.cgi.eoss.ftep.orchestrator;

import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        PersistenceConfig.class
})
@ComponentScan("com.cgi.eoss.ftep.orchestrator")
public class OrchestratorConfig {

    @Bean
    public Path cacheRoot(@Value("${ftep.orchestrator.cache.baseDir:/data/cache/dl}") String cacheRoot) {
        return Paths.get(cacheRoot);
    }

    @Bean
    public Path jobEnvironmentRoot(@Value("${ftep.orchestrator.jobEnv.baseDir:/data/cache/jobs}") String jobEnvRoot) {
        return Paths.get(jobEnvRoot);
    }

    @Bean
    public HttpTransport httpTransport() {
        return new NetHttpTransport();
    }

}
