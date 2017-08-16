package com.cgi.eoss.ftep.worker.io;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class S1CEDADownloader implements Downloader {

    private static final String CEDA_SEARCH_URI_FORMAT = "http://opensearch.ceda.ac.uk/opensearch/json?maximumRecords=1&name=${PRODUCT_ID}";
    private static final String CEDA_FTP_URI_FORMAT = "ftp://ftp.ceda.ac.uk${DIRECTORY}/${DATAFILE}";

    private final FtpDownloader ftpDownloader;
    private final OkHttpClient httpClient;

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("sentinel1");
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        String productId = uri.getPath().substring(1);
        HttpUrl httpUrl = HttpUrl.parse(StrSubstitutor.replace(CEDA_SEARCH_URI_FORMAT, ImmutableMap.of("PRODUCT_ID", productId)));

        Request request = new Request.Builder().url(httpUrl).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for CEDA search: {}", response.toString());
                throw new ServiceIoException("Unexpected HTTP response from CEDA: " + response.message());
            }

            String responseBody = response.body().string();
            String productDirectory = JsonPath.read(responseBody, "$.rows[0].file.directory");
            String productDataFile = JsonPath.read(responseBody, "$.rows[0].file.data_file");

            URI ftpUri = new URI(StrSubstitutor.replace(CEDA_FTP_URI_FORMAT, ImmutableMap.of("DIRECTORY", productDirectory, "DATAFILE", productDataFile)));
            return ftpDownloader.download(targetDir, ftpUri);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

}
