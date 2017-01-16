package com.cgi.eoss.ftep.orchestrator.io;

import com.cgi.eoss.ftep.model.internal.Credentials;
import com.cgi.eoss.ftep.orchestrator.data.CredentialsDataService;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 */
public class FtpDownloaderTest {

    @Mock
    private CredentialsDataService credentialsDataService;

    private Path targetPath;

    private FileSystem fs;

    private Downloader dl;

    private FakeFtpServer ftpServer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.fs = Jimfs.newFileSystem(Configuration.unix());
        this.targetPath = this.fs.getPath("/target");
        Files.createDirectories(targetPath);

        this.ftpServer = buildFakeFtpServer();
        this.ftpServer.start();

        this.dl = new FtpDownloader(credentialsDataService);
    }

    @Test
    public void test() throws Exception {
        String ftpHost = "localhost:" + ftpServer.getServerControlPort();

        when(credentialsDataService.getCredentials(any())).thenReturn(Credentials.builder()
                .username("ftpuser")
                .password("ftppass")
                .build());

        Path download = dl.download(targetPath, URI.create("ftp://" + ftpHost + "/data/testfile.txt"));

        assertThat(download, is(targetPath.resolve("testfile.txt")));
        assertThat(Files.readAllLines(download), is(ImmutableList.of("foo bar baz")));
    }

    private FakeFtpServer buildFakeFtpServer() throws Exception {
        FakeFtpServer ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(getRandomPort());

        ftpServer.addUserAccount(new UserAccount("ftpuser", "ftppass", "/data"));

        org.mockftpserver.fake.filesystem.FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data"));
        FileEntry testFile = new FileEntry("/data/testfile.txt");
        byte[] testFileBytes = Files.readAllBytes(Paths.get(FtpDownloaderTest.class.getResource("/testfile.txt").toURI()));
        testFile.setContents(testFileBytes);
        fileSystem.add(testFile);
        ftpServer.setFileSystem(fileSystem);

        return ftpServer;
    }

    private int getRandomPort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

}