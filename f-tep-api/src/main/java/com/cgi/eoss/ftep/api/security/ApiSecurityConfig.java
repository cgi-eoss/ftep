package com.cgi.eoss.ftep.api.security;

import com.cgi.eoss.ftep.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;

import javax.cache.configuration.MutableConfiguration;
import java.io.Serializable;

@Configuration
@ConditionalOnProperty("ftep.api.security.enabled")
@EnableWebSecurity
public class ApiSecurityConfig {

    private static final String ACL_CACHE_NAME = "acls";

    @Bean
    public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter(
            @Value("${ftep.api.security.username-request-attribute:REMOTE_USER}") String usernameRequestAttribute,
            FtepUserDetailsService ftepUserDetailsService) {
        return new WebSecurityConfigurerAdapter() {
            @Override
            protected void configure(HttpSecurity httpSecurity) throws Exception {
                // Extracts the shibboleth user id from the request
                RequestAttributeAuthenticationFilter filter = new RequestAttributeAuthenticationFilter();
                filter.setAuthenticationManager(authenticationManager());
                filter.setPrincipalEnvironmentVariable(usernameRequestAttribute);

                // Handles any authentication exceptions, and translates to a simple 403
                // There is no login redirection as we are expecting pre-auth
                ExceptionTranslationFilter exceptionTranslationFilter = new ExceptionTranslationFilter(new Http403ForbiddenEntryPoint());

                // TODO Enable per-object ACLs
                httpSecurity
                        .addFilterBefore(exceptionTranslationFilter, RequestAttributeAuthenticationFilter.class)
                        .addFilter(filter)
                        .authorizeRequests()
                        .anyRequest().permitAll();
                httpSecurity
                        .csrf().disable();
            }

            @Override
            protected void configure(AuthenticationManagerBuilder auth) throws Exception {
                PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
                authenticationProvider.setPreAuthenticatedUserDetailsService(ftepUserDetailsService);
                auth.authenticationProvider(authenticationProvider);
            }
        };
    }

    @Bean
    public JCacheManagerCustomizer jCacheManagerCustomizer() {
        return cacheManager -> cacheManager.createCache(ACL_CACHE_NAME, new MutableConfiguration<Serializable, Object>());
    }

    @Bean
    public SpringCacheBasedAclCache aclCache(CacheManager cacheManager, AuditLogger auditLogger) {
        Cache cache = cacheManager.getCache(ACL_CACHE_NAME);
        return new SpringCacheBasedAclCache(cache,
                new DefaultPermissionGrantingStrategy(auditLogger),
                new AclAuthorizationStrategyImpl(Role.ADMIN)
        );
    }

}
