package com.cgi.eoss.ftep.io.download;

import com.cgi.eoss.ftep.model.DownloaderCredentials;
import com.cgi.eoss.ftep.persistence.service.DownloaderCredentialsDataService;
import com.cgi.eoss.ftep.persistence.service.RpcCredentialsService;
import com.cgi.eoss.ftep.rpc.CredentialsServiceGrpc;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CEDADownloaderTest {
    @Mock
    private FtepServerClient ftepServerClient;
    @Mock
    private DownloaderCredentialsDataService credentialsDataService;

    private MockWebServer webServer;

    private FakeFtpServer ftpServer;

    private Path targetPath;

    private Path cacheRoot;

    private FileSystem fs;

    private CEDADownloader dl;

    private Server server;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.fs = Jimfs.newFileSystem(Configuration.unix());
        this.targetPath = this.fs.getPath("/target");
        Files.createDirectories(targetPath);
        this.cacheRoot = this.fs.getPath("/cache");
        Files.createDirectories(cacheRoot);

        this.webServer = buildFakeWebServer();
        this.webServer.start();

        this.ftpServer = buildFakeFtpServer();
        this.ftpServer.start();

        DownloaderFacade downloaderFacade = new CachingSymlinkDownloaderFacade(cacheRoot);

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(getClass().getName()).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(getClass().getName()).directExecutor();
        RpcCredentialsService rpcCredentialsService = new RpcCredentialsService(credentialsDataService);
        inProcessServerBuilder.addService(rpcCredentialsService);
        server = inProcessServerBuilder.build().start();

        CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsService = CredentialsServiceGrpc.newBlockingStub(channelBuilder.build());
        when(ftepServerClient.credentialsServiceBlockingStub()).thenReturn(credentialsService);

        when(credentialsDataService.getByHost(any())).thenReturn(DownloaderCredentials.basicBuilder()
                .username("ftpuser")
                .password("ftppass")
                .build());

        this.dl = new CEDADownloader(downloaderFacade, new OkHttpClient.Builder().build(), this.webServer.url(""), "ftp://localhost:" + ftpServer.getServerControlPort(), ProtocolPriority.builder().build());

        new FtpDownloader(downloaderFacade, ftepServerClient).postConstruct();
    }

    @After
    public void tearDown() throws IOException {
        webServer.shutdown();
        ftpServer.stop();
        server.shutdownNow();
    }

    @Test
    public void download() throws Exception {
        URI product = URI.create("sentinel2:///S2A_MSIL1C_20170428T164901_N0205_R083_T15QVU_20170428T164855");

        // Trimmed example of CEDA opensearch response
        webServer.enqueue(new MockResponse().setBody("{\n" +
                "    \"rows\": [\n" +
                "        {\n" +
                "            \"file\": {\n" +
                "                \"directory\": \"/data\",\n" +
                "                \"data_file\": \"singleFile.zip\",\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}"));

        Path download = dl.download(targetPath, product);
        List<String> result = Files.walk(download, FileVisitOption.FOLLOW_LINKS).map(Path::toString).sorted().collect(Collectors.toList());
        assertThat(result, is(ImmutableList.of(
                "/target/S2A_MSIL1C_20170428T164901_N0205_R083_T15QVU_20170428T164855",
                "/target/S2A_MSIL1C_20170428T164901_N0205_R083_T15QVU_20170428T164855/.uri",
                "/target/S2A_MSIL1C_20170428T164901_N0205_R083_T15QVU_20170428T164855/file1"
        )));
        Path productLink = targetPath.resolve("S2A_MSIL1C_20170428T164901_N0205_R083_T15QVU_20170428T164855");
        assertThat(Files.isSymbolicLink(productLink), is(true));
        assertThat(Files.readSymbolicLink(productLink), is(cacheRoot.resolve(hash("ftp://localhost:" + ftpServer.getServerControlPort() + "/data/singleFile.zip"))));
    }

    private MockWebServer buildFakeWebServer() throws Exception {
        return new MockWebServer();
    }

    private FakeFtpServer buildFakeFtpServer() throws Exception {
        FakeFtpServer ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(getRandomPort());

        ftpServer.addUserAccount(new UserAccount("ftpuser", "ftppass", "/data"));

        org.mockftpserver.fake.filesystem.FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data"));

        FileEntry testFile = new FileEntry("/data/singleFile.zip");
        byte[] testFileBytes = Files.readAllBytes(Paths.get(FtpDownloaderTest.class.getResource("/singleFile.zip").toURI()));
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

    private String hash(Object object) {
        return Hashing.sha1().hashString(object.toString(), Charset.forName("UTF-8")).toString();
    }

}
