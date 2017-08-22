package com.cgi.eoss.ftep.io.download;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * <p>Facade to {@link Downloader} implementations. Offers retry logic across multiple providers for a given URI
 * scheme.</p>
 */
public interface DownloaderFacade {
    Path download(URI uri, Path targetDir);

    Map<URI, Path> download(Map<URI, Optional<Path>> uriPathMap);

    boolean isSupportedProtocol(String scheme);

    void registerDownloader(Downloader downloader);

    void unregisterDownloader(Downloader downloader);

    void cleanUp(URI uri);
}
