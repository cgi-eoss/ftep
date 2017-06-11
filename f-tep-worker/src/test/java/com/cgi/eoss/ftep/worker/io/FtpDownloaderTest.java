package com.cgi.eoss.ftep.worker.io;

import com.cgi.eoss.ftep.model.DownloaderCredentials;
import com.cgi.eoss.ftep.persistence.service.DownloaderCredentialsDataService;
import com.cgi.eoss.ftep.persistence.service.RpcCredentialsService;
import com.cgi.eoss.ftep.rpc.CredentialsServiceGrpc;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.ServerImpl;
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

    private FileSystem fs;

    private Downloader dl;

    private FakeFtpServer ftpServer;

    private ServerImpl server;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.fs = Jimfs.newFileSystem(Configuration.unix());
        this.targetPath = this.fs.getPath("/target");
        Files.createDirectories(targetPath);

        this.ftpServer = buildFakeFtpServer();
        this.ftpServer.start();

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(getClass().getName()).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(getClass().getName()).directExecutor();
        RpcCredentialsService rpcCredentialsService = new RpcCredentialsService(credentialsDataService);
        inProcessServerBuilder.addService(rpcCredentialsService);
        server = inProcessServerBuilder.build().start();

        CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsService = CredentialsServiceGrpc.newBlockingStub(channelBuilder.build());
        when(ftepServerClient.credentialsServiceBlockingStub()).thenReturn(credentialsService);

        this.dl = new FtpDownloader(ftepServerClient);
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void test() throws Exception {
        String ftpHost = "localhost:" + ftpServer.getServerControlPort();

        when(credentialsDataService.getByHost(any())).thenReturn(DownloaderCredentials.basicBuilder()
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