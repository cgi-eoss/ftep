package com.cgi.eoss.ftep.io.download;

import com.cgi.eoss.ftep.model.DownloaderCredentials;
import com.cgi.eoss.ftep.persistence.service.DownloaderCredentialsDataService;
import com.cgi.eoss.ftep.persistence.service.RpcCredentialsService;
import com.cgi.eoss.ftep.rpc.CredentialsServiceGrpc;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 */
public class FtpDownloaderTest {
    @Mock
    private FtepServerClient ftepServerClient;
    @Mock
    private DownloaderCredentialsDataService credentialsDataService;

    private Path targetPath;

    private Path cacheRoot;

    private FileSystem fs;

    private FtpDownloader dl;

    private FakeFtpServer ftpServer;

    private Server server;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.fs = Jimfs.newFileSystem(Configuration.unix());
        this.targetPath = this.fs.getPath("/target");
        Files.createDirectories(targetPath);
        this.cacheRoot = this.fs.getPath("/cache");
        Files.createDirectories(cacheRoot);

        this.ftpServer = buildFakeFtpServer();
        this.ftpServer.start();

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(getClass().getName()).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(getClass().getName()).directExecutor();
        RpcCredentialsService rpcCredentialsService = new RpcCredentialsService(credentialsDataService);
        inProcessServerBuilder.addService(rpcCredentialsService);
        server = inProcessServerBuilder.build().start();

        CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsService = CredentialsServiceGrpc.newBlockingStub(channelBuilder.build());
        when(ftepServerClient.credentialsServiceBlockingStub()).thenReturn(credentialsService);

        this.dl = new FtpDownloader(new CachingSymlinkDownloaderFacade(cacheRoot), ftepServerClient);
        this.dl.postConstruct();
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void testDownload() throws Exception {
        String ftpHost = "localhost:" + ftpServer.getServerControlPort();

        when(credentialsDataService.getByHost(any())).thenReturn(Optional.of(DownloaderCredentials.builder()
                .username("ftpuser")
                .password("ftppass")
                .build()));

        Path download = dl.download(targetPath, URI.create("ftp://" + ftpHost + "/data/testfile.txt"));

        assertThat(download, is(targetPath.resolve("testfile.txt")));
        assertThat(Files.readAllLines(download), is(ImmutableList.of("foo bar baz")));
    }

    @Test
    public void testDownloadDirectory() throws Exception {
        String ftpHost = "localhost:" + ftpServer.getServerControlPort();

        when(credentialsDataService.getByHost(any())).thenReturn(Optional.of(DownloaderCredentials.builder()
                .username("ftpuser")
                .password("ftppass")
                .build()));

        Path download = dl.download(targetPath, URI.create("ftp://" + ftpHost + "/recursiveData"));

        assertThat(download, is(targetPath));
        Set<String> result = Files.walk(targetPath).filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toSet());
        assertThat(result, is(ImmutableSet.of(
                "/target/testfile.txt",
                "/target/subdir/testfile.txt",
                "/target/subdir/subsubdir/testfile.txt"
        )));
        assertThat(Files.readAllLines(targetPath.resolve("testfile.txt")), is(ImmutableList.of("foo bar baz")));
        assertThat(Files.readAllLines(targetPath.resolve("subdir/testfile.txt")), is(ImmutableList.of("foo bar baz")));
        assertThat(Files.readAllLines(targetPath.resolve("subdir/subsubdir/testfile.txt")), is(ImmutableList.of("foo bar baz")));
    }

    private FakeFtpServer buildFakeFtpServer() throws Exception {
        FakeFtpServer ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(getRandomPort());

        ftpServer.addUserAccount(new UserAccount("ftpuser", "ftppass", "/data"));

        org.mockftpserver.fake.filesystem.FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data"));
        fileSystem.add(new DirectoryEntry("/recursiveData"));
        fileSystem.add(new DirectoryEntry("/recursiveData/subdir"));
        fileSystem.add(new DirectoryEntry("/recursiveData/subdir/subsubdir"));

        FileEntry testFile = new FileEntry("/data/testfile.txt");
        byte[] testFileBytes = Files.readAllBytes(Paths.get(FtpDownloaderTest.class.getResource("/testfile.txt").toURI()));
        testFile.setContents(testFileBytes);
        fileSystem.add(testFile);

        FileEntry testFile2 = new FileEntry("/recursiveData/testfile.txt");
        byte[] testFile2Bytes = Files.readAllBytes(Paths.get(FtpDownloaderTest.class.getResource("/testfile.txt").toURI()));
        testFile2.setContents(testFile2Bytes);
        fileSystem.add(testFile2);

        FileEntry testFile3 = new FileEntry("/recursiveData/subdir/testfile.txt");
        byte[] testFile3Bytes = Files.readAllBytes(Paths.get(FtpDownloaderTest.class.getResource("/testfile.txt").toURI()));
        testFile3.setContents(testFile3Bytes);
        fileSystem.add(testFile3);

        FileEntry testFile4 = new FileEntry("/recursiveData/subdir/subsubdir/testfile.txt");
        byte[] testFile4Bytes = Files.readAllBytes(Paths.get(FtpDownloaderTest.class.getResource("/testfile.txt").toURI()));
        testFile4.setContents(testFile4Bytes);
        fileSystem.add(testFile4);

        ftpServer.setFileSystem(fileSystem);

        return ftpServer;
    }

    private int getRandomPort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

}