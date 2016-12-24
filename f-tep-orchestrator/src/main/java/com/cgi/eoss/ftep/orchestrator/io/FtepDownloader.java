package com.cgi.eoss.ftep.orchestrator.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class FtepDownloader implements Downloader {
    @Override
    public void download(Path target, URI uri) throws IOException {
        throw new UnsupportedOperationException("ftep:// URIs are not yet supported");
    }
}
