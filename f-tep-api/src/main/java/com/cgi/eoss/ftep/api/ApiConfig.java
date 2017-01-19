package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

import static org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        RepositoryRestMvcConfiguration.class,
        PersistenceConfig.class
})
@EnableJpaRepositories(basePackages = "com.cgi.eoss.ftep.api")
@ComponentScan("com.cgi.eoss.ftep.api")
public class ApiConfig {

    private static final String API_BASE_PATH = "/api";

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer() {
        return new RepositoryRestConfigurerAdapter() {
            @Override
            public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
                config.setBasePath(API_BASE_PATH);
                config.setRepositoryDetectionStrategy(ANNOTATED);
            }
        };
    }
}
