package com.cgi.eoss.ftep.orchestrator.io;

import lombok.experimental.UtilityClass;

import java.net.URI;

/**
 * <p>Factory to produce instances of {@link Downloader} for different URIs. The specific implementations returned may
 * differ depending on configuration, allowing environment-specific handling of a given URI.</p>
 */
@UtilityClass
public class DownloaderFactory {

    private static final String PROTOCOL_HANDLER_PROP_BASE = "ftep.protocol-handler.";

    static {
        // TODO Make these properties configurable for different environments
        System.setProperty("ftep.protocol-handler.ftep", "com.cgi.eoss.ftep.orchestrator.io.FtepDownloader");
        System.setProperty("ftep.protocol-handler.http", "com.cgi.eoss.ftep.orchestrator.io.HttpFtpDownloader");
        System.setProperty("ftep.protocol-handler.ftp", "com.cgi.eoss.ftep.orchestrator.io.HttpFtpDownloader");
        System.setProperty("ftep.protocol-handler.s2", "com.cgi.eoss.ftep.orchestrator.io.S2CEDADownloader");
    }

    /**
     * @return A {@link Downloader} capable of resolving and retrieving the given URI in the current environment.
     */
    public static Downloader getHandler(URI uri) {
        try {
            String handlerProperty = PROTOCOL_HANDLER_PROP_BASE + uri.getScheme();
            String handlerClass = System.getProperty(handlerProperty);
            Class<?> cls = Class.forName(handlerClass);
            if (Downloader.class.isAssignableFrom(cls)) {
                return (Downloader) cls.getConstructor().newInstance();
            } else {
                throw new ClassCastException("Invalid protocol handler class: " + handlerClass);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
