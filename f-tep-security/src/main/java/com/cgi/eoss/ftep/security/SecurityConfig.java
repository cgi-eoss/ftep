package com.cgi.eoss.ftep.security;

import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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

import javax.cache.configuration.MutableConfiguration;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.SQLException;

@Configuration
@Import({
        CacheAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class,

        PersistenceConfig.class
})
@EnableCaching
@ComponentScan(basePackageClasses = SecurityConfig.class)
@Log4j2
public class SecurityConfig {

    private static final String ACL_CACHE_NAME = "acls";

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
