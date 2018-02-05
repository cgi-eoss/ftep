package com.cgi.eoss.ftep.io.download;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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

    /**
     * <p>Build a stream representing the resolved value of a URI. Typically necessary if the URI requires complex
     * resolution or expansion to process, e.g. databaskets -> their contents' URIs.</p>
     * <p><strong>Only one {@link Downloader} implementation will be used to perform this behaviour.</strong> The first
     * (see {@link DownloaderUriComparator}) matching Downloader will be used. Do not use
     * this functionality for general-purpose URL translation.</p>
     *
     * @param uri The URI to be expanded or resolved.
     * @return A stream of the URI's resolved representation elements. By default, a single-element stream of the input URI.
     */
    default Stream<URI> resolveUri(URI uri) {
        return Stream.of(uri);
    }
}
