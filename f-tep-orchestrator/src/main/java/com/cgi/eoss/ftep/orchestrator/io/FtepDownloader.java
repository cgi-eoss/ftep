package com.cgi.eoss.ftep.orchestrator.io;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

@Slf4j
public class FtepDownloader implements Downloader {
    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);
        throw new UnsupportedOperationException("ftep:// URIs are not yet supported");
    }
}
