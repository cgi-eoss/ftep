package com.cgi.eoss.ftep.api.security.dev;

import com.cgi.eoss.ftep.api.security.FtepUserDetailsService;
import com.cgi.eoss.ftep.api.security.FtepWebAuthenticationDetailsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;

@Configuration
@ConditionalOnProperty(value = "ftep.api.security.mode", havingValue = "DEVELOPMENT_BECOME_ANY_USER")
@EnableWebSecurity
@EnableGlobalAuthentication
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class ApiSecurityDevConfig {

    @Bean
    public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter(
            @Value("${ftep.api.security.username-request-attribute:REMOTE_USER}") String usernameRequestAttribute,
            @Value("${ftep.api.security.email-request-header:REMOTE_EMAIL}") String emailRequestHeader) {
        return new WebSecurityConfigurerAdapter() {
            @Override
            protected void configure(HttpSecurity httpSecurity) throws Exception {
                // Maps a session attribute to a request attribute, allowing us to choose a user id per session
                SessionUserAttributeInjectorFilter sessionUserAttributeInjector = new SessionUserAttributeInjectorFilter(usernameRequestAttribute);

                // Retrieve the "sso" user from the request attributes (not from headers, as in SSO mode)
                RequestAttributeAuthenticationFilter filter = new RequestAttributeAuthenticationFilter();
                filter.setAuthenticationManager(authenticationManager());
                filter.setPrincipalEnvironmentVariable(usernameRequestAttribute);
                filter.setAuthenticationDetailsSource(new FtepWebAuthenticationDetailsSource(emailRequestHeader));
                filter.setExceptionIfVariableMissing(false);

                ExceptionTranslationFilter exceptionTranslationFilter = new ExceptionTranslationFilter(new Http403ForbiddenEntryPoint());

                httpSecurity
                        .addFilterBefore(sessionUserAttributeInjector, RequestAttributeAuthenticationFilter.class)
                        .addFilterBefore(exceptionTranslationFilter, SessionUserAttributeInjectorFilter.class)
                        .addFilter(filter)
                        .authorizeRequests()
                        .antMatchers("/**/dev/user/become/*").permitAll()
                        .anyRequest().authenticated();
                httpSecurity
                        .csrf().disable();
                httpSecurity
                        .cors();
            }
        };
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, FtepUserDetailsService ftepUserDetailsService) {
        PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(ftepUserDetailsService);
        auth.authenticationProvider(authenticationProvider);
    }

    @Bean
    public AclPermissionEvaluator aclPermissionEvaluator(AclService aclService) {
        return new AclPermissionEvaluator(aclService);
    }

    @Bean
    public MethodSecurityExpressionHandler createExpressionHandler(AclPermissionEvaluator aclPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(aclPermissionEvaluator);
        return expressionHandler;
    }

}
