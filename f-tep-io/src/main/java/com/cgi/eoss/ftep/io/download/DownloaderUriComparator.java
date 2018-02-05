package com.cgi.eoss.ftep.io.download;

import java.net.URI;
import java.util.Comparator;

public final class DownloaderUriComparator implements Comparator<Downloader> {
    private final URI uri;

    DownloaderUriComparator(URI uri) {
        this.uri = uri;
    }

    @Override
    public int compare(Downloader o1, Downloader o2) {
        return Integer.compare(o1.getPriority(uri), o2.getPriority(uri));
    }
}
