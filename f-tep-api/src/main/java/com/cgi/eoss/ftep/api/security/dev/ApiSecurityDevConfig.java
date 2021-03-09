package com.cgi.eoss.ftep.api.security.dev;

import com.cgi.eoss.ftep.security.FtepUserDetailsService;
import com.cgi.eoss.ftep.security.FtepWebAuthenticationDetailsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;
import org.springframework.stereotype.Component;

@Configuration
@ConditionalOnProperty(value = "ftep.api.security.mode", havingValue = "DEVELOPMENT_BECOME_ANY_USER")
@EnableWebSecurity
@EnableGlobalAuthentication
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class ApiSecurityDevConfig {

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, FtepUserDetailsService ftepUserDetailsService) {
        PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(ftepUserDetailsService);
        auth.authenticationProvider(authenticationProvider);
    }

    @Bean
    public MethodSecurityExpressionHandler createExpressionHandler(AclPermissionEvaluator aclPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(aclPermissionEvaluator);
        return expressionHandler;
    }

    @Component
    @ConditionalOnProperty(value = "ftep.api.security.mode", havingValue = "DEVELOPMENT_BECOME_ANY_USER")
    @Order(SecurityProperties.BASIC_AUTH_ORDER - 2)
    public static class ApiDevSecurityConfigurer extends WebSecurityConfigurerAdapter {

        private final String usernameRequestHeader;
        private final String emailRequestHeader;
        private final String organisationRequestHeader;
        private final String countryRequestHeader;

        @Autowired
        public ApiDevSecurityConfigurer(
                @Value("${ftep.api.security.username-request-header:REMOTE_USER}") String usernameRequestHeader,
                @Value("${ftep.api.security.email-request-header:REMOTE_EMAIL}") String emailRequestHeader,
                @Value("${ftep.api.security.organisation-request-header:REMOTE_ORGANISATION}") String organisationRequestHeader,
                @Value("${ftep.api.security.country-request-header:REMOTE_COUNTRY}") String countryRequestHeader
        ) {
            this.usernameRequestHeader = usernameRequestHeader;
            this.emailRequestHeader = emailRequestHeader;
            this.organisationRequestHeader = organisationRequestHeader;
            this.countryRequestHeader = countryRequestHeader;
        }

        @Override
        protected void configure(HttpSecurity httpSecurity) throws Exception {
            // Maps a session attribute to a request attribute, allowing us to choose a user id per session
            SessionUserAttributeInjectorFilter sessionUserAttributeInjector = new SessionUserAttributeInjectorFilter(usernameRequestHeader);

            // Retrieve the "sso" user from the request attributes (not from headers, as in SSO mode)
            RequestAttributeAuthenticationFilter filter = new RequestAttributeAuthenticationFilter();
            filter.setAuthenticationManager(authenticationManager());
            filter.setPrincipalEnvironmentVariable(usernameRequestHeader);
            filter.setAuthenticationDetailsSource(new FtepWebAuthenticationDetailsSource(
                    emailRequestHeader,
                    organisationRequestHeader,
                    countryRequestHeader));
            filter.setExceptionIfVariableMissing(false);

            ExceptionTranslationFilter exceptionTranslationFilter = new ExceptionTranslationFilter(new Http403ForbiddenEntryPoint());

            httpSecurity
                    .addFilterBefore(sessionUserAttributeInjector, RequestAttributeAuthenticationFilter.class)
                    .addFilterBefore(exceptionTranslationFilter, SessionUserAttributeInjectorFilter.class)
                    .addFilter(filter);
            httpSecurity.authorizeRequests()
                    .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                    .antMatchers("/**/dev/user/become/*").permitAll()
                    .anyRequest().authenticated();
            httpSecurity
                    .csrf().disable();
            httpSecurity
                    .cors();
            // TODO Make DEV security mode STATELESS for consistency with SSO
            httpSecurity
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
        }
    }

}
