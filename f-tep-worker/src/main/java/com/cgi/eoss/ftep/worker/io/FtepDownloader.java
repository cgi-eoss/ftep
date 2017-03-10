package com.cgi.eoss.ftep.worker.io;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

@Log4j2
public class FtepDownloader implements Downloader {
    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);
        throw new UnsupportedOperationException("ftep:// URIs are not yet supported");
    }
}
