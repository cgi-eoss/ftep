package com.cgi.eoss.ftep.orchestrator.io;

import com.cgi.eoss.ftep.model.internal.Credentials;
import com.cgi.eoss.ftep.persistence.service.DatasourceDataService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class FtpDownloader implements Downloader {
    private static final int CONNECT_TIMEOUT = 2000;
    private static final String FTPS_SCHEME = "ftps";

    private final DatasourceDataService datasourceDataService;

    FtpDownloader(DatasourceDataService datasourceDataService) {
        this.datasourceDataService = datasourceDataService;
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        FTPClient ftpClient = FTPS_SCHEME.equals(uri.getScheme()) ? new FTPSClient() : new FTPClient();
        ftpClient.setConnectTimeout(CONNECT_TIMEOUT);

        try {
            if (uri.getPort() == -1) {
                ftpClient.connect(uri.getHost());
            } else {
                ftpClient.connect(uri.getHost(), uri.getPort());
            }
            Credentials creds = datasourceDataService.getCredentials(uri.getHost());
            if (creds.isBasicAuth()) {
                ftpClient.login(creds.getUsername(), creds.getPassword());
            }
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            Path outputFile = targetDir.resolve(Paths.get(uri.getPath()).getFileName().toString());
            try (OutputStream os = Files.newOutputStream(outputFile)) {
                boolean success = ftpClient.retrieveFile(uri.getPath(), os);
                if (!success) {
                    LOG.error("FTP download error: {}", ftpClient.getReplyString());
                    throw new ServiceIoException("Failed to download via FTP: " + uri);
                }
            }
            LOG.info("Successfully downloaded via FTP: {}", outputFile);
            return outputFile;
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
    }
}
