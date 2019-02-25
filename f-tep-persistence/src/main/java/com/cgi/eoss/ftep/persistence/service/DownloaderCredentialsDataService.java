package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.DownloaderCredentials;

import java.util.Optional;

public interface DownloaderCredentialsDataService extends
        FtepEntityDataService<DownloaderCredentials> {
    /**
     * @param host The hostname for which credentials are required.
     * @return The credentials required to download from the given host, or null if no credentials were found for the
     * host.
     */
    Optional<DownloaderCredentials> getByHost(String host);
}
