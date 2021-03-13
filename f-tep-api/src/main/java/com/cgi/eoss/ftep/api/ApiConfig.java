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
import com.cgi.eoss.ftep.search.SearchConfig;
import com.cgi.eoss.ftep.security.SecurityConfig;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        SearchConfig.class,
        SecurityConfig.class
})
@EnableJpaRepositories(basePackageClasses = ApiConfig.class)
@ComponentScan(basePackageClasses = ApiConfig.class)
@Log4j2
public class ApiConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder.modulesToInstall(
                new GuavaModule(),
                new Hibernate5Module(),
                new JavaTimeModule());
    }

    @Value("${ftep.api.basePath:/api}")
    private String apiBasePath;

    @Bean
    public WebMvcRegistrations webMvcRegistrationsHandlerMapping() {
        return new WebMvcRegistrations() {
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

    // Handle X-Forwarded-* headers
    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ForwardedHeaderFilter());
        return bean;
    }

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer(@Value("${ftep.api.basePath:/api}") String apiBasePath) {
        return new RepositoryRestConfigurerAdapter() {
            @Override
            public void configureHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
                messageConverters.add(new ResourceHttpMessageConverter(true));
                messageConverters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
            }

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
    @Component
    @ConditionalOnProperty(value = "ftep.api.security.mode", havingValue = "NONE", matchIfMissing = true)
    @Order(SecurityProperties.BASIC_AUTH_ORDER - 2)
    public static class ApiNoneSecurityConfigurer extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity httpSecurity) throws Exception {
            httpSecurity
                    .authorizeRequests()
                    .anyRequest().permitAll();
            httpSecurity
                    .csrf().disable();
            httpSecurity
                    .cors();
            httpSecurity
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }
    }

}
