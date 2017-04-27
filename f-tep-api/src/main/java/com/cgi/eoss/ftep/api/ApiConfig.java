package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.api.security.FtepPermission;
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
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcRegistrationsAdapter;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.cache.configuration.MutableConfiguration;
import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.SQLException;

import static org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED;

@Configuration
@Import({
        CacheAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class,
        RepositoryRestMvcAutoConfiguration.class,
        WebMvcAutoConfiguration.EnableWebMvcConfiguration.class,

        CatalogueConfig.class,
        CostingConfig.class,
        InProcessRpcConfig.class,
        OrchestratorConfig.class,
        PersistenceConfig.class
})
@EnableCaching
@EnableJpaRepositories(basePackageClasses = ApiConfig.class)
@ComponentScan(basePackageClasses = ApiConfig.class)
@Log4j2
public class ApiConfig {

    private static final String ACL_CACHE_NAME = "acls";

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
            }
        };
    }

    @Bean
    public JCacheManagerCustomizer jCacheManagerCustomizer() {
        return cacheManager -> {
            if (!Iterables.contains(cacheManager.getCacheNames(), ACL_CACHE_NAME)) {
                cacheManager.createCache(ACL_CACHE_NAME, new MutableConfiguration<Serializable, Object>());
            }
        };
    }

    @Bean
    public SpringCacheBasedAclCache aclCache(CacheManager cacheManager, AuditLogger auditLogger, AclAuthorizationStrategy aclAuthorizationStrategy) {
        Cache cache = cacheManager.getCache(ACL_CACHE_NAME);
        return new SpringCacheBasedAclCache(cache,
                new DefaultPermissionGrantingStrategy(auditLogger),
                aclAuthorizationStrategy
        );
    }

    @Bean
    public AclAuthorizationStrategy aclAuthorizationStrategy() {
        return new AclAuthorizationStrategyImpl(FtepPermission.PUBLIC);
    }

    @Bean
    public LookupStrategy lookupStrategy(DataSource dataSource, AclCache aclCache, AclAuthorizationStrategy aclAuthorizationStrategy, AuditLogger auditLogger) {
        return new BasicLookupStrategy(dataSource, aclCache, aclAuthorizationStrategy, auditLogger);
    }

    @Bean
    public MutableAclService mutableAclService(DataSource dataSource, LookupStrategy lookupStrategy, AclCache aclCache) throws SQLException {
        JdbcMutableAclService aclService = new JdbcMutableAclService(dataSource, lookupStrategy, aclCache);

        // Set the ACL ID query methods appropriately for specific database types
        switch (dataSource.getConnection().getMetaData().getDatabaseProductName()) {
            case "PostgreSQL":
                LOG.info("Setting ACL ID query methods for PostgreSQL");
                aclService.setSidIdentityQuery("SELECT currval('acl_sid_id_seq');");
                aclService.setClassIdentityQuery("SELECT currval('acl_class_id_seq');");
                break;
            default:
                LOG.info("Leaving ACL ID query methods as default");
        }

        return aclService;
    }

    @Bean
    public AclPermissionEvaluator aclPermissionEvaluator(AclService aclService) {
        return new AclPermissionEvaluator(aclService);
    }

}
