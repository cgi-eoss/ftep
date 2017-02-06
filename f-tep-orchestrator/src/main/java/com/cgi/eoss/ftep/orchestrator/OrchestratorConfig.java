package com.cgi.eoss.ftep.orchestrator;

import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
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
@ComponentScan(basePackageClasses = OrchestratorConfig.class)
public class OrchestratorConfig {

    @Bean
    public Integer cacheConcurrencyLevel(@Value("${ftep.orchestrator.cache.concurrency:4}") int concurrencyLevel) {
        return concurrencyLevel;
    }

    @Bean
    public Integer cacheMaxWeight(@Value("${ftep.orchestrator.cache.maxWeight:1024}") int maximumWeight) {
        return maximumWeight;
    }

    @Bean
    public Path cacheRoot(@Value("${ftep.orchestrator.cache.baseDir:/data/cache/dl}") String cacheRoot) {
        return Paths.get(cacheRoot);
    }

    @Bean
    public Path jobEnvironmentRoot(@Value("${ftep.orchestrator.jobEnv.baseDir:/data/cache/jobs}") String jobEnvRoot) {
        return Paths.get(jobEnvRoot);
    }

}
