package com.cgi.eoss.ftep.worker.io;

import com.cgi.eoss.ftep.rpc.Credentials;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.GetCredentialsParams;
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            downloadFile(ftpClient, Paths.get(uri.getPath()), outputFile);
            return outputFile;
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
    }

    public Path downloadDirectory(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading FTP directory: {}", uri);

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

            Path currentRelativeDir = Paths.get("");
            downloadRecursive(ftpClient, Paths.get(uri.getPath()), currentRelativeDir, targetDir);
            return targetDir;
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
    }

    private void downloadRecursive(FTPClient ftpClient, Path rootDir, Path currentRelativeDir, Path targetDir) throws IOException {
        Path currentFtpDir = rootDir.resolve(currentRelativeDir);
        Path currentTargetDir = Files.createDirectories(targetDir.resolve(currentRelativeDir.toString()));

        FTPFile[] currentFiles = ftpClient.listFiles(currentFtpDir.toString());

        Map<Boolean, List<FTPFile>> currentPathEntries = Arrays.stream(currentFiles)
                .filter(f -> !f.getName().equals(".") && !f.getName().equals(".."))
                .collect(Collectors.partitioningBy(FTPFile::isDirectory));

        currentPathEntries.get(true).forEach(Unchecked.consumer(f -> downloadRecursive(ftpClient, rootDir, currentRelativeDir.resolve(f.getName()), targetDir)));
        currentPathEntries.get(false).forEach(Unchecked.consumer(f -> downloadFile(ftpClient, rootDir.resolve(currentRelativeDir).resolve(f.getName()), currentTargetDir.resolve(f.getName()))));
    }

    private void downloadFile(FTPClient ftpClient, Path ftpPath, Path targetFile) throws IOException {
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(targetFile))) {
            boolean success = ftpClient.retrieveFile(ftpPath.toString(), os);
            if (!success) {
                LOG.error("FTP download error: {}", ftpClient.getReplyString());
                throw new ServiceIoException("Failed to download via FTP: " + ftpClient.getPassiveHost() + ftpPath);
            }
            LOG.info("Successfully downloaded via FTP: {}", targetFile);
        }
    }
}
