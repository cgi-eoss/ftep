package com.cgi.eoss.ftep.io.download;

import lombok.Getter;
import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@Getter
final class Oauth2PasswordGrantRequest extends AbstractOAuth2AuthorizationGrantRequest {
    private final String authEndpoint;
    private final String clientId;
    private final String username;
    private final String password;

    protected Oauth2PasswordGrantRequest(String authEndpoint, String clientId, String username, String password) {
        super(new AuthorizationGrantType("password"));
        this.authEndpoint = authEndpoint;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
    }
}
