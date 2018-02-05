package com.cgi.eoss.ftep.io.download;

import com.cgi.eoss.ftep.io.ServiceIoException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.text.StrSubstitutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;

@Log4j2
public class CEDADownloader implements Downloader {

    private static final String CEDA_FTP_DIRECTORY_URI_FORMAT = "${FTP_URL_BASE}${DIRECTORY}";
    private static final String CEDA_FTP_FILE_URI_FORMAT = "${FTP_URL_BASE}${DIRECTORY}/${DATAFILE}";

    private final HttpUrl cedaSearchUrl;
    private final String ftpUrlBase;
    private final OkHttpClient httpClient;
    private final DownloaderFacade downloaderFacade;
    private final ProtocolPriority protocolPriority;

    public CEDADownloader(DownloaderFacade downloaderFacade, OkHttpClient httpClient, HttpUrl cedaSearchUrl, String ftpUrlBase, ProtocolPriority protocolPriority) {
        this.downloaderFacade = downloaderFacade;
        this.httpClient = httpClient;
        this.cedaSearchUrl = cedaSearchUrl;
        this.ftpUrlBase = ftpUrlBase;
        this.protocolPriority = protocolPriority;
    }

    @PostConstruct
    public void postConstruct() {
        downloaderFacade.registerDownloader(this);
    }

    @PreDestroy
    public void preDestroy() {
        downloaderFacade.unregisterDownloader(this);
    }

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("sentinel1", "sentinel2", "landsat");
    }

    @Override
    public int getPriority(URI uri) {
        return protocolPriority.get(uri.getScheme());
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        // Trim the leading slash from the path
        String productId = uri.getPath().substring(1);
        HttpUrl httpUrl = cedaSearchUrl.newBuilder()
                .addQueryParameter("maximumRecords", "1")
                .addQueryParameter("name", productId)
                .build();

        Request request = new Request.Builder().url(httpUrl).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for CEDA search: {}", response.toString());
                throw new ServiceIoException("Unexpected HTTP response from CEDA: " + response.message());
            }

            // TODO Ask CEDA to load an archived product from tape

            String responseBody = response.body().string();
            String productDirectory = JsonPath.read(responseBody, "$.rows[0].file.directory");
            String productDataFile = JsonPath.read(responseBody, "$.rows[0].file.data_file");

            URI ftpUri;
            if (uri.getScheme().equals("landsat")) {
                ftpUri = new URI(StrSubstitutor.replace(CEDA_FTP_DIRECTORY_URI_FORMAT, ImmutableMap.of("FTP_URL_BASE", ftpUrlBase, "DIRECTORY", productDirectory)));
            } else {
                ftpUri = new URI(StrSubstitutor.replace(CEDA_FTP_FILE_URI_FORMAT, ImmutableMap.of("FTP_URL_BASE", ftpUrlBase, "DIRECTORY", productDirectory, "DATAFILE", productDataFile)));
            }
            LOG.info("Resolved product URI {} into CEDA URI {}", uri, ftpUri);
            return downloaderFacade.download(ftpUri, targetDir.resolve(productId));
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

}
