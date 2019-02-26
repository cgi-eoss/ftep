package com.cgi.eoss.ftep.io.download;

import com.cgi.eoss.ftep.io.ServiceIoException;
import com.cgi.eoss.ftep.rpc.Credentials;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.GetCredentialsParams;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class CreodiasHttpAuthenticator implements Authenticator {
    private static final int MAX_RETRIES = 3;

    private final FtepServerClient ftepServerClient;
    private final PasswordTokenResponseClient tokenClient;
    private final String authEndpoint;
    private final String clientId;

    private OAuth2AccessTokenResponse tokenResponse;

    public CreodiasHttpAuthenticator(FtepServerClient ftepServerClient, String authEndpoint, String clientId) {
        this.ftepServerClient = ftepServerClient;
        this.authEndpoint = authEndpoint;
        this.clientId = clientId;
        this.tokenClient = new PasswordTokenResponseClient();
    }

    @Override
    public Request authenticate(Route route, Response response) {
        try {
            HttpUrl url = response.request().url();

            if (responseCount(response) >= MAX_RETRIES) {
                LOG.warn("Failed authentication for {} {} times, aborting", url, MAX_RETRIES);
                return null;
            }

            LOG.debug("Authenticating request to: {}", route);

            if (tokenResponse == null || Optional.ofNullable(tokenResponse.getAccessToken().getExpiresAt()).map(expiry -> Instant.now().isAfter(expiry)).orElse(true)) {
                tokenResponse = tokenClient.getTokenResponse(buildAuthRequest(url));
                // TODO use the refresh token
            }

            return response.request().newBuilder()
                    .url(response.request().url().newBuilder()
                            .addQueryParameter("token", tokenResponse.getAccessToken().getTokenValue())
                            .build())
                    .build();
        } catch (Exception e) {
            LOG.error("Failed to authenticate with CREODIAS", e);
            return null;
        }
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

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    // Adapted from org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient
    private static final class PasswordTokenResponseClient implements OAuth2AccessTokenResponseClient<Oauth2PasswordGrantRequest> {
        private final RestTemplate restOperations;

        PasswordTokenResponseClient() {
            RestTemplate restTemplate = new RestTemplate(Arrays.asList(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()));
            restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
            this.restOperations = restTemplate;
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
