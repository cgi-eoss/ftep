package com.cgi.eoss.ftep.worker.io;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Ordering;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class DownloaderFacadeImpl implements DownloaderFacade {

    private final ListMultimap<String, Downloader> downloaders = MultimapBuilder.hashKeys().arrayListValues().build();

    public DownloaderFacadeImpl(List<Downloader> availableDownloaders) {
        Multimap<String, Downloader> unsortedDownloaders = MultimapBuilder.hashKeys().hashSetValues().build();
        availableDownloaders.forEach(d -> d.getProtocols().forEach(p -> unsortedDownloaders.put(p, d)));

        for (Map.Entry<String, Collection<Downloader>> unsortedEntry : unsortedDownloaders.asMap().entrySet()) {
            String protocol = unsortedEntry.getKey();
            Collection<Downloader> protocolDownloaders = unsortedEntry.getValue();
            List<Downloader> sortedDownloaders = Ordering.from(new DownloaderSchemeComparator(protocol)).immutableSortedCopy(protocolDownloaders);
            LOG.info("Registered downloaders for protocol {} in order: {}", protocol, sortedDownloaders);
            this.downloaders.replaceValues(protocol, sortedDownloaders);
        }
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        List<Downloader> schemeDownloaders = downloaders.get(uri.getScheme());
        for (Downloader downloader : schemeDownloaders) {
            try {
                LOG.debug("Attempting download with {} for uri: {}", downloader, uri);
                return downloader.download(targetDir, uri);
            } catch (Exception e) {
                LOG.error("Failed to download with {} uri: {}", downloader, uri, e);
            }
        }
        throw new ServiceIoException("No downloader was able to process the URI: " + uri);
    }

    @Override
    public boolean isSupportedProtocol(String scheme) {
        return downloaders.containsKey(scheme);
    }

    private class DownloaderSchemeComparator implements Comparator<Downloader> {
        private final String scheme;

        private DownloaderSchemeComparator(String scheme) {
            this.scheme = scheme;
        }

        @Override
        public int compare(Downloader o1, Downloader o2) {
            return Integer.compare(o1.getPriority(this.scheme), o2.getPriority(this.scheme));
        }
    }
}
