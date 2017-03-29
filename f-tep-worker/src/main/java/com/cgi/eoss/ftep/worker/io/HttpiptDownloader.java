package com.cgi.eoss.ftep.worker.io;

import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;

/**
 * <p>Downloader for resolving "httpipt" URLs. Resolves the URL to the actual IPT URL, then passes to our {@link
 * IptDownloader} which handles the IPT authentication process and download.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class HttpiptDownloader implements Downloader {

    // TODO Add this to the credentials/datasource object
    @Value("${ftep.worker.downloader.ipt.downloadBaseUrl:https://static.eocloud.eu/v1/AUTH_8f07679eeb0a43b19b33669a4c888c45/eorepo}")
    private String iptDownloadBaseUrl;

    private final IptDownloader iptDownloader;

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("httpipt");
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        String productPath = uri.toString().startsWith("httpipt:///")
                ? uri.getPath()
                : uri.getHost() + uri.getPath(); // Deal with technically-incorrect paths from the search service

        HttpUrl realUrl = HttpUrl.parse(iptDownloadBaseUrl).newBuilder()
                .addPathSegments(productPath)
                .build();

        LOG.debug("Resolved IPT download URL: {}", realUrl);

        return iptDownloader.download(targetDir, realUrl.uri());
    }

}
