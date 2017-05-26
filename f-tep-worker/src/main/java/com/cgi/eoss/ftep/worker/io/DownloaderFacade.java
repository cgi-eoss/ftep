package com.cgi.eoss.ftep.worker.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * <p>Facade to {@link Downloader} implementations. Offers retry logic across multiple providers for a given URI
 * scheme.</p>
 */
public interface DownloaderFacade {
    Path download(Path targetDir, URI uri) throws IOException;

    boolean isSupportedProtocol(String scheme);
}
