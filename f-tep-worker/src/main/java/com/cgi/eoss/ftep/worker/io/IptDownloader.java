package com.cgi.eoss.ftep.worker.io;

import com.cgi.eoss.ftep.rpc.Credentials;
import com.cgi.eoss.ftep.rpc.GetCredentialsParams;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * <p>Downloader for resolving "httpipt" URLs. Uses IPT's token authentication process.</p>
 */
@Component
@Log4j2
public class IptDownloader implements Downloader {

    // TODO Add this to the credentials/datasource object (?)
    @Value("${ftep.worker.downloader.ipt.authEndpoint:https://finder.eocloud.eu/resto/api/authidentity}")
    private String authEndpoint;

    // TODO Add this to the credentials/datasource object
    @Value("${ftep.worker.downloader.ipt.authDomain:__secret__}")
    private String authDomain;

    private final FtepServerClient ftepServerClient;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    @Autowired
    IptDownloader(OkHttpClient okHttpClient, FtepServerClient ftepServerClient) {
        this.client = okHttpClient;
        this.ftepServerClient = ftepServerClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of();
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        // IPT downloading is two-step:
        //   1. Call an authentication endpoint to get a token
        //   2. Add the token identity to the download operation as a parameter

        Credentials credentials = ftepServerClient.credentialsServiceBlockingStub().getCredentials(GetCredentialsParams.newBuilder().setHost(uri.getHost()).build());

        HttpUrl downloadUrl = HttpUrl.get(uri).newBuilder()
                .addQueryParameter("token", getAuthToken(credentials))
                .build();

        LOG.debug("Resolved IPT download URL with auth token: {}", downloadUrl);

        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new ServiceIoException("Unsuccessful HTTP response: " + response);
        }

        String filename = Iterables.getLast(downloadUrl.pathSegments());
        Path outputFile = targetDir.resolve(filename);

        try (InputStream is = response.body().byteStream();
             OutputStream os = Files.newOutputStream(outputFile)) {
            ByteStreams.copy(is, os);
        }
        response.close();

        LOG.info("Successfully downloaded via IPT: {}", outputFile);
        return outputFile;
    }

    private String getAuthToken(Credentials credentials) throws IOException {
        Request authRequest = new Request.Builder()
                .url(authEndpoint)
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("domainName", authDomain)
                        .addFormDataPart("userName", credentials.getUsername())
                        .addFormDataPart("userPass", credentials.getPassword())
                        .build())
                .build();

        try (Response response = client.newCall(authRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new ServiceIoException("Unsuccessful IPT login: " + response);
            }
            IptTokenResponse iptTokenResponse = objectMapper.readValue(response.body().string(), IptTokenResponse.class);
            LOG.debug("Logged into IPT as user '{}' (id: {}) with tokenIdentity '{}'", iptTokenResponse.getUserName(), iptTokenResponse.getUserId(), iptTokenResponse.getTokenIdentity());
            return iptTokenResponse.getTokenIdentity();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class IptTokenResponse {
        private String token;
        private String tokenIdentity;
        private String userName;
        private String userId;
        private String objectStoreUrl;
    }

}
