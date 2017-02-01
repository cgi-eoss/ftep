package com.cgi.eoss.ftep.orchestrator.io;

import com.cgi.eoss.ftep.model.DownloaderCredentials;
import com.cgi.eoss.ftep.persistence.service.DownloaderCredentialsDataService;
import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class HttpDownloader implements Downloader {

    private static final String FILENAME_HEADER = "Content-Disposition";
    private static final Pattern FILENAME_PATTERN = Pattern.compile(".*filename=\"(.*)\".*");

    private final OkHttpClient client;

    HttpDownloader(DownloaderCredentialsDataService downloaderCredentialsDataService) {
        this.client = new OkHttpClient.Builder()
                .authenticator(new FtepAuthenticator(downloaderCredentialsDataService))
                .build();
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        Request request = new Request.Builder().url(uri.toURL()).build();
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new ServiceIoException("Unsuccessful HTTP response: " + response);
        }

        String filename = getFilename(uri, response.headers(FILENAME_HEADER));

        Path outputFile = targetDir.resolve(filename);

        try (InputStream is = response.body().byteStream();
             OutputStream os = Files.newOutputStream(outputFile)) {
            ByteStreams.copy(is, os);
        }
        LOG.info("Successfully downloaded via HTTP: {}", outputFile);
        return outputFile;
    }

    private String getFilename(URI uri, List<String> headers) {
        return headers.stream()
                .map(FILENAME_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(m -> m.group(1))
                .findAny()
                .orElse(Paths.get(uri.getPath()).getFileName().toString());
    }

    private static final class FtepAuthenticator implements Authenticator {
        private static final int MAX_RETRIES = 3;
        private final DownloaderCredentialsDataService credentialsDataService;

        private FtepAuthenticator(DownloaderCredentialsDataService credentialsDataService) {
            this.credentialsDataService = credentialsDataService;
        }

        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            HttpUrl url = route.address().url();

            if (responseCount(response) >= MAX_RETRIES) {
                LOG.error("Failed authentication for {} {} times, aborting", url, MAX_RETRIES);
                return null;
            }

            DownloaderCredentials creds = credentialsDataService.getByHost(url.host());

            if (creds.getType() == DownloaderCredentials.Type.BASIC) {
                String credHeader = okhttp3.Credentials.basic(creds.getUsername(), creds.getPassword());
                return response.request().newBuilder()
                        .header("Authorization", credHeader)
                        .build();
            } else {
                LOG.error("Authentication required for {}, but no basic credentials found, aborting", url);
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

}
