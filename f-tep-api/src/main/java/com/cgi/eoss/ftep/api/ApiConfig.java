package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import static org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        RepositoryRestMvcConfiguration.class,
        PersistenceConfig.class
})
@EnableJpaRepositories(basePackages = "com.cgi.eoss.ftep.api")
@EnableWebSecurity
@ComponentScan("com.cgi.eoss.ftep.api")
public class ApiConfig {

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer(@Value("${ftep.api.base-path:/api}") String apiBasePath) {
        return new RepositoryRestConfigurerAdapter() {
            @Override
            public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
                config.setBasePath(apiBasePath);
                config.setRepositoryDetectionStrategy(ANNOTATED);
            }
        };
    }

    @Bean
    public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
        return new WebSecurityConfigurerAdapter() {
            @Override
            protected void configure(HttpSecurity httpSecurity) throws Exception {
                // TODO Require shibboleth pre-auth via RequestAttributeAuthenticationFilter
                // http://docs.spring.io/spring-security/site/docs/4.2.1.RELEASE/reference/htmlsingle/#preauth
                // TODO Enable per-object ACLs
                httpSecurity
                        .authorizeRequests()
                        .anyRequest().anonymous();
                httpSecurity
                        .csrf().disable();
            }
        };
    }

}
