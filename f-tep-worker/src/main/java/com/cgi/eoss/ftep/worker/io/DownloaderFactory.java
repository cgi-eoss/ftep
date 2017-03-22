package com.cgi.eoss.ftep.worker.io;

import com.cgi.eoss.ftep.rpc.CredentialsServiceGrpc;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;

/**
 * <p>Factory to produce instances of {@link Downloader} for different URIs. The specific implementations returned may
 * differ depending on configuration, allowing environment-specific handling of a given URI.</p>
 */
@Service
public class DownloaderFactory {

    // TODO Make these handlers configurable for different environments, and allow priority loading
    private final Map<String, Downloader> downloaders = Maps.newHashMap();

    @Autowired
    public DownloaderFactory(FtepDownloader ftepDownloader, CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsService) {
        FtpDownloader ftpDownloader = new FtpDownloader(credentialsService);
        HttpDownloader httpDownloader = new HttpDownloader(credentialsService);
        S2CEDADownloader s2CEDADownloader = new S2CEDADownloader(ftpDownloader);

        registerDownloader("ftep", ftepDownloader);
        registerDownloader("ftp", ftpDownloader);
        registerDownloader("ftps", ftpDownloader);
        registerDownloader("http", httpDownloader);
        registerDownloader("https", httpDownloader);
        registerDownloader("s2", s2CEDADownloader);
    }

    private void registerDownloader(String scheme, Downloader downloader) {
        downloaders.put(scheme, downloader);
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
