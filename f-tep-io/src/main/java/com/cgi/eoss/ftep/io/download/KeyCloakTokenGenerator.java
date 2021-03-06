package com.cgi.eoss.ftep.io.download;

import com.cgi.eoss.ftep.io.ServiceIoException;
import com.cgi.eoss.ftep.rpc.Credentials;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.GetCredentialsParams;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Log4j2
public class KeyCloakTokenGenerator {

    private final PasswordTokenResponseClient tokenClient;
    private final FtepServerClient ftepServerClient;

    private OAuth2AccessTokenResponse tokenResponse;

    private final String authEndpoint;
    private final String clientId;

    public KeyCloakTokenGenerator(OkHttpClient okHttpClient, FtepServerClient ftepServerClient, String authEndpoint, String clientId) {
        this.tokenClient = new PasswordTokenResponseClient(okHttpClient);
        this.ftepServerClient = ftepServerClient;
        this.authEndpoint = authEndpoint;
        this.clientId = clientId;
    }

    public OAuth2AccessTokenResponse getKeyCloakAuthenticationToken(HttpUrl url) {

        if (tokenResponse == null || Optional.ofNullable(tokenResponse.getAccessToken().getExpiresAt()).map(expiry -> Instant.now().isAfter(expiry)).orElse(true)) {
            tokenResponse = tokenClient.getTokenResponse(buildAuthRequest(url));
        }

        return tokenResponse;
    }

    private Oauth2PasswordGrantRequest buildAuthRequest(HttpUrl url) {
        Credentials creds = ftepServerClient.credentialsServiceBlockingStub().getCredentials(GetCredentialsParams.newBuilder().setHost(url.host()).build());
        if (creds.getType() == Credentials.Type.BASIC) {
            return new Oauth2PasswordGrantRequest(authEndpoint, clientId, creds.getUsername(), creds.getPassword());
        } else {
            LOG.error("Authentication required for {}, but no basic credentials found, aborting", url);
            throw new ServiceIoException("No BASIC credentials found for CREODIAS finder");
        }
    }

    // Adapted from org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient
    private static final class PasswordTokenResponseClient implements OAuth2AccessTokenResponseClient<Oauth2PasswordGrantRequest> {
        private final RestTemplate restOperations;

        PasswordTokenResponseClient(OkHttpClient okHttpClient) {
            this.restOperations = new RestTemplateBuilder()
                    .additionalMessageConverters(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter())
                    .setConnectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                    .setReadTimeout(Duration.of(10, ChronoUnit.SECONDS))
                    .errorHandler(new OAuth2ErrorResponseErrorHandler())
                    .requestFactory(() -> new OkHttp3ClientHttpRequestFactory(okHttpClient))
                    .build();
        }

        @Override
        public OAuth2AccessTokenResponse getTokenResponse(Oauth2PasswordGrantRequest authorizationGrantRequest) {
            MultiValueMap<String, String> formParameters = new LinkedMultiValueMap<>();
            formParameters.add(OAuth2ParameterNames.GRANT_TYPE, authorizationGrantRequest.getGrantType().getValue());
            formParameters.add(OAuth2ParameterNames.CLIENT_ID, authorizationGrantRequest.getClientId());
            formParameters.add("username", authorizationGrantRequest.getUsername());
            formParameters.add("password", authorizationGrantRequest.getPassword());
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

            RequestEntity<?> request = new RequestEntity<>(formParameters, headers, HttpMethod.POST, UriComponentsBuilder.fromUriString(authorizationGrantRequest.getAuthEndpoint()).build().toUri());

            ResponseEntity<OAuth2AccessTokenResponse> response;
            try {
                response = this.restOperations.exchange(request, OAuth2AccessTokenResponse.class);
            } catch (RestClientException ex) {
                OAuth2Error oauth2Error = new OAuth2Error("invalid_token_response", "An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: " + ex.getMessage(), null);
                throw new OAuth2AuthorizationException(oauth2Error, ex);
            }

            OAuth2AccessTokenResponse tokenResponse = response.getBody();

            if (CollectionUtils.isEmpty(tokenResponse.getAccessToken().getScopes())) {
                // As per spec, in Section 5.1 Successful Access Token Response
                // https://tools.ietf.org/html/rfc6749#section-5.1
                // If AccessTokenResponse.scope is empty, then default to the scope
                // originally requested by the client in the Token Request
                tokenResponse = OAuth2AccessTokenResponse.withResponse(tokenResponse).build();
            }

            return tokenResponse;
        }
    }

}
