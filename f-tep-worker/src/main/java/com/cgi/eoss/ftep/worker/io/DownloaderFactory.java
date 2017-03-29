package com.cgi.eoss.ftep.worker.io;

import com.google.common.collect.Maps;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * <p>Factory to produce instances of {@link Downloader} for different URIs. The specific implementations returned may
 * differ depending on configuration, allowing environment-specific handling of a given URI.</p>
 */
@Service
@Log4j2
public class DownloaderFactory {

    // TODO Make these handlers configurable for different environments, and allow priority loading
    private final Map<String, Downloader> downloaders = Maps.newHashMap();

    @Autowired
    public DownloaderFactory(List<Downloader> downloaders) {
        downloaders.forEach(this::registerDownloader);
    }

    private void registerDownloader(Downloader downloader) {
        downloader.getProtocols().stream()
                .peek(p -> LOG.info("Registering downloader for protocol '{}': {}", p, downloader))
                .forEach(p -> downloaders.put(p, downloader));
    }

    /**
     * @return A {@link Downloader} capable of resolving and retrieving the given URI in the current environment.
     */
    public Downloader getDownloader(URI uri) {
        try {
            return downloaders.get(uri.getScheme());
        } catch (Exception e) {
            throw new ServiceIoException(e);
        }
    }

    /**
     * <p>Return true if a downloader for the given URI scheme (i.e. protocol) is registered.</p>
     */
    public boolean isSupportedProtocol(String scheme) {
        return downloaders.keySet().contains(scheme);
    }

}
