package com.cgi.eoss.ftep.data.manager.core;

import java.nio.file.Path;

/**
 */
public final class DownloaderConfiguration {

    private final Path downloadDir;
    private final Path downloadScript;

    public DownloaderConfiguration(Path downloadDir, Path downloadScript) {
        this.downloadDir = downloadDir;
        this.downloadScript = downloadScript;
    }

    public Path getDownloadDir() {
        return downloadDir;
    }

    public Path getDownloadScript() {
        return downloadScript;
    }
}
