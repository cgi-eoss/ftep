package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.catalogue.CatalogueConfig;
import com.cgi.eoss.ftep.costing.CostingConfig;
import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.orchestrator.OrchestratorConfig;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.cgi.eoss.ftep.rpc.InProcessRpcConfig;
import com.cgi.eoss.ftep.security.SecurityConfig;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcRegistrationsAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

import static org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        RepositoryRestMvcAutoConfiguration.class,
        WebMvcAutoConfiguration.EnableWebMvcConfiguration.class,

        CatalogueConfig.class,
        CostingConfig.class,
        InProcessRpcConfig.class,
        OrchestratorConfig.class,
        PersistenceConfig.class,
        SecurityConfig.class
})
@EnableJpaRepositories(basePackageClasses = ApiConfig.class)
@EnableWebMvc
@ComponentScan(basePackageClasses = ApiConfig.class)
@Log4j2
public class ApiConfig {

    @Bean
    public GuavaModule jacksonGuavaModule() {
        return new GuavaModule();
    }

    @Bean
    public Hibernate5Module jacksonHibernateModule() {
        return new Hibernate5Module();
    }

    @Bean
    public WebMvcRegistrationsAdapter webMvcRegistrationsHandlerMapping(@Value("${ftep.api.basePath:/api}") String apiBasePath) {
        return new WebMvcRegistrationsAdapter() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new RequestMappingHandlerMapping() {
                    @Override
                    protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
                        Class<?> beanType = method.getDeclaringClass();
                        RestController restApiController = beanType.getAnnotation(RestController.class);
                        if (restApiController != null) {
                            PatternsRequestCondition apiPattern = new PatternsRequestCondition(apiBasePath).combine(mapping.getPatternsCondition());

                            mapping = new RequestMappingInfo(mapping.getName(), apiPattern,
                                    mapping.getMethodsCondition(), mapping.getParamsCondition(),
                                    mapping.getHeadersCondition(), mapping.getConsumesCondition(),
                                    mapping.getProducesCondition(), mapping.getCustomCondition());
                        }

                        super.registerHandlerMethod(handler, method, mapping);
                    }
                };
            }
        };
    }

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer(@Value("${ftep.api.basePath:/api}") String apiBasePath) {
        return new RepositoryRestConfigurerAdapter() {
            @Override
            public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
                config.setRepositoryDetectionStrategy(ANNOTATED);
                config.setBasePath(apiBasePath);

                // Ensure that the id attribute is returned for all API-mapped types
                ImmutableList.of(Group.class, JobConfig.class, Job.class, FtepService.class, FtepServiceContextFile.class, User.class, FtepFile.class, Databasket.class, Project.class)
                        .forEach(config::exposeIdsFor);
            }
        };
    }

    @Bean
    @ConditionalOnProperty(value = "ftep.api.security.cors.enabled", havingValue = "true", matchIfMissing = true)
    public CorsConfigurationSource corsConfigurationSource(@Value("${ftep.api.basePath:/api}") String apiBasePath) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration(apiBasePath + "/**", config);
        return source;
    }

    @Bean
    @ConditionalOnProperty(value = "ftep.api.security.cors.enabled", havingValue = "false")
    public CorsConfigurationSource disabledCorsConfiguration(@Value("${ftep.api.basePath:/api}") String apiBasePath) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        source.registerCorsConfiguration(apiBasePath + "/**", config);
        return source;
    }

    // Spring Security configuration for anonymous/open access
    @Bean
    @ConditionalOnProperty(value = "ftep.api.security.mode", havingValue = "NONE", matchIfMissing = true)
    public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
        return new WebSecurityConfigurerAdapter() {
            @Override
            protected void configure(HttpSecurity httpSecurity) throws Exception {
                httpSecurity
                        .authorizeRequests()
                        .anyRequest().anonymous();
                httpSecurity
                        .csrf().disable();
                httpSecurity
                        .cors();
                httpSecurity
                        .sessionManagement()
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            }
        };
    }

}
