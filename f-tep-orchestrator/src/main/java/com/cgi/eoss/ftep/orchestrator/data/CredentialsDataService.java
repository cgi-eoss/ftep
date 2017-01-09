package com.cgi.eoss.ftep.orchestrator.data;

import com.cgi.eoss.ftep.model.FtepDatasource;
import com.cgi.eoss.ftep.model.internal.Credentials;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

// TODO Remove this when direct java API is functional
@Slf4j
public class CredentialsDataService {

    private static final String POLICY_CREDENTIALS = "credentials";
    private static final String POLICY_X509 = "x509";
    private final JsonParser jsonParser = new JsonParser();

    private final FtepJsonApi api;

    public CredentialsDataService(FtepJsonApi api) {
        this.api = api;
    }

    public Credentials getCredentials(String host) throws IOException {
        FtepDatasource datasource = api.getDataSource(host);

        String policy = datasource.getPolicy();
        String credentialsString = datasource.getCredentialsData();
        JsonObject credentials = jsonParser.parse(credentialsString).getAsJsonObject();

        switch (policy) {
            case POLICY_CREDENTIALS:
                return Credentials.builder()
                        .username(credentials.get("user").getAsString())
                        .password(credentials.get("password").getAsString())
                        .build();
            case POLICY_X509:
                return Credentials.builder()
                        .certificate(credentials.get("certpath").getAsString())
                        .build();
            default:
                return Credentials.builder().build();
        }
    }
}
