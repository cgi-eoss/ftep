package com.cgi.eoss.ftep.io.download;

import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.GetCredentialsParams;
import lombok.extern.log4j.Log4j2;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

import javax.annotation.Nullable;
import java.io.IOException;

@Log4j2
public class IptEodataServerAuthenticator implements Authenticator {

    private static final int MAX_RETRIES = 3;

    private final FtepServerClient ftepServerClient;

    public IptEodataServerAuthenticator(FtepServerClient ftepServerClient) {
        this.ftepServerClient = ftepServerClient;
    }

    @Nullable
    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (responseCount(response) >= MAX_RETRIES) {
            LOG.warn("Failed authentication for {} {} times, aborting", response.request().url(), MAX_RETRIES);
            return null;
        }

        com.cgi.eoss.ftep.rpc.Credentials credentials = ftepServerClient.credentialsServiceBlockingStub().getCredentials(
                GetCredentialsParams.newBuilder().setHost(response.request().url().host()).build());

        return response.request().newBuilder()
                .header("Authorization", Credentials.basic(credentials.getUsername(), credentials.getPassword()))
                .build();
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

}
