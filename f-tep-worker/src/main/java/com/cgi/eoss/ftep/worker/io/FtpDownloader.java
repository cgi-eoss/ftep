package com.cgi.eoss.ftep.worker.io;

import com.cgi.eoss.ftep.rpc.Credentials;
import com.cgi.eoss.ftep.rpc.GetCredentialsParams;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class FtpDownloader implements Downloader {
    private static final int CONNECT_TIMEOUT = 2000;
    private static final String FTPS_SCHEME = "ftps";

    private final FtepServerClient ftepServerClient;

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("ftp", "ftps");
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
            Credentials creds = ftepServerClient.credentialsServiceBlockingStub().getCredentials(GetCredentialsParams.newBuilder().setHost(uri.getHost()).build());
            if (creds.getType() == Credentials.Type.BASIC) {
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
