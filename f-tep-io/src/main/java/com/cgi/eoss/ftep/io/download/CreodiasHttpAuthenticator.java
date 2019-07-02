package com.cgi.eoss.ftep.io.download;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;


@Slf4j
public class CreodiasHttpAuthenticator implements Authenticator {
    private static final int MAX_RETRIES = 3;

    @Getter
    private final KeyCloakTokenGenerator keyCloakTokenGenerator;

    private OAuth2AccessTokenResponse tokenResponse;

    public CreodiasHttpAuthenticator(KeyCloakTokenGenerator keyCloakTokenGenerator) {
        this.keyCloakTokenGenerator = keyCloakTokenGenerator;
    }

    @Override
    public Request authenticate(Route route, Response response) {
        try {
            HttpUrl url = response.request().url();
            if (responseCount(response) >= MAX_RETRIES) {
                LOG.warn("Failed authentication for {} {} times, aborting", url, MAX_RETRIES);
                return null;
            }

            tokenResponse = keyCloakTokenGenerator.getKeyCloakAuthenticationToken(url);

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

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

}
