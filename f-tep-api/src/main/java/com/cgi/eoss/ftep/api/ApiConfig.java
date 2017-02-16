package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import java.util.Arrays;

import static org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED;

@Configuration
@Import({
        CacheAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class,
        RepositoryRestMvcConfiguration.class,
        PersistenceConfig.class
})
@EnableCaching
@EnableJpaRepositories(basePackageClasses = ApiConfig.class)
@ComponentScan(basePackageClasses = ApiConfig.class)
public class ApiConfig {

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer(@Value("${ftep.api.base-path:/api}") String apiBasePath) {
        return new RepositoryRestConfigurerAdapter() {
            @Override
            public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
                config.setBasePath(apiBasePath);
                config.setRepositoryDetectionStrategy(ANNOTATED);
                // Ensure that the id attribute is returned for all API-mapped types
                Arrays.stream(new Class<?>[]{Group.class, JobConfig.class, Job.class, FtepService.class, User.class})
                        .forEach(config::exposeIdsFor);
            }
        };
    }

    // Spring Security configuration for anonymous/open access, used when ftep.api.security.enabled is false
    @Bean
    @ConditionalOnProperty(value = "ftep.api.security.enabled", havingValue = "false", matchIfMissing = true)
    public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
        return new WebSecurityConfigurerAdapter() {
            @Override
            protected void configure(HttpSecurity httpSecurity) throws Exception {
                httpSecurity
                        .authorizeRequests()
                        .anyRequest().anonymous();
                httpSecurity
                        .csrf().disable();
            }
        };
    }

}
