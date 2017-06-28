package com.cgi.eoss.ftep.worker.io;

import com.cgi.eoss.ftep.catalogue.CatalogueServiceImpl;
import com.cgi.eoss.ftep.model.DownloaderCredentials;
import com.cgi.eoss.ftep.persistence.service.DownloaderCredentialsDataService;
import com.cgi.eoss.ftep.persistence.service.RpcCredentialsService;
import com.cgi.eoss.ftep.rpc.CredentialsServiceGrpc;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.catalogue.CatalogueServiceGrpc;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.ServerImpl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
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

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CachingSymlinkIOManagerIT {
    @Mock
    private FtepDownloader ftepDownloader;

    @Mock
    private FtepServerClient ftepServerClient;

    @Mock
    private DownloaderCredentialsDataService credentialsDataService;

    private FakeFtpServer ftpServer;

    private MockWebServer webServer;

    private FileSystem fs;

    private Path cacheDir;

    private Path workDir;

    private ServiceInputOutputManager ioManager;

    private ServerImpl server;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fs = Jimfs.newFileSystem(Configuration.unix());
        cacheDir = this.fs.getPath("/cache");
        workDir = this.fs.getPath("/work");
        Files.createDirectories(cacheDir);
        Files.createDirectories(workDir);

        when(credentialsDataService.getByHost(any())).thenReturn(
                DownloaderCredentials.basicBuilder().username("ftepuser").password("fteppass").build());

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(getClass().getName()).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(getClass().getName()).directExecutor();

        RpcCredentialsService rpcCredentialsService = new RpcCredentialsService(credentialsDataService);
        CatalogueServiceImpl rpcCatalogueService = mock(CatalogueServiceImpl.class);

        inProcessServerBuilder.addService(rpcCredentialsService);
        inProcessServerBuilder.addService(rpcCatalogueService);
        server = inProcessServerBuilder.build().start();

        ftpServer = buildFtpServer();
        ftpServer.start();
        webServer = buildWebServer();
        webServer.start();

        CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsService = CredentialsServiceGrpc.newBlockingStub(channelBuilder.build());
        when(ftepServerClient.credentialsServiceBlockingStub()).thenReturn(credentialsService);

        CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueService = CatalogueServiceGrpc.newBlockingStub(channelBuilder.build());
        when(ftepServerClient.catalogueServiceBlockingStub()).thenReturn(catalogueService);


        ioManager = new CachingSymlinkIOManager(ftepServerClient, cacheDir, new DownloaderFacadeImpl(
                ImmutableList.of(ftepDownloader, new FtpDownloader(ftepServerClient), new HttpDownloader(ftepServerClient, new OkHttpClient.Builder().build()))));
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void test() throws Exception {
        // Basically what Worker does to process job inputs
        String httpzipUri = webServer.url("/singleFile.zip").toString();
        String ftptxtUri = "ftp://localhost:" + ftpServer.getServerControlPort() + "/data/testfile.txt";
        Multimap<String, String> inputs = ImmutableMultimap.of(
                "httpzip", httpzipUri,
                "ftptxt", ftptxtUri
        );

        for (Map.Entry<String, String> e : inputs.entries()) {
            Path subdirPath = workDir.resolve(e.getKey());
            ioManager.prepareInput(subdirPath, ImmutableSet.of(URI.create(e.getValue())));
        }

        Set<String> cacheResult = Files.walk(cacheDir).map(Path::toString).collect(Collectors.toSet());
        String ftptxtUriHash = hash(ftptxtUri);
        String httpzipUriHash = hash(httpzipUri);
        assertThat(cacheResult, is(ImmutableSet.of(
                "/cache",
                "/cache/" + ftptxtUriHash,
                "/cache/" + ftptxtUriHash + "/.uri",
                "/cache/" + ftptxtUriHash + "/testfile.txt",
                "/cache/" + httpzipUriHash,
                "/cache/" + httpzipUriHash + "/.uri",
                "/cache/" + httpzipUriHash + "/file1"
        )));

        assertThat(Files.isSymbolicLink(workDir.resolve("ftptxt")), is(true));
        assertThat(Files.readSymbolicLink(workDir.resolve("ftptxt")), is(cacheDir.resolve(ftptxtUriHash)));
        assertThat(Files.isSymbolicLink(workDir.resolve("httpzip")), is(true));
        assertThat(Files.readSymbolicLink(workDir.resolve("httpzip")), is(cacheDir.resolve(httpzipUriHash)));

        Set<String> result = Files.walk(workDir, FOLLOW_LINKS).map(Path::toString).collect(Collectors.toSet());
        assertThat(result, is(ImmutableSet.of(
                "/work",
                "/work/ftptxt",
                "/work/ftptxt/.uri",
                "/work/ftptxt/testfile.txt",
                "/work/httpzip",
                "/work/httpzip/.uri",
                "/work/httpzip/file1"
        )));
        assertThat(Files.readAllLines(workDir.resolve("ftptxt/.uri")), is(ImmutableList.of(ftptxtUri)));
        assertThat(Files.readAllLines(workDir.resolve("httpzip/.uri")), is(ImmutableList.of(httpzipUri)));
    }

    @Test
    public void testCacheReuse() throws Exception {
        URI uri = webServer.url("/singleFile.zip").uri();

        Path firstTarget = workDir.resolve("httpzip-1");
        Path secondTarget = workDir.resolve("httpzip-2");

        ioManager.prepareInput(firstTarget, ImmutableSet.of(uri));
        ioManager.prepareInput(secondTarget, ImmutableSet.of(uri));

        assertThat(Files.readSymbolicLink(workDir.resolve("httpzip-1")), is(cacheDir.resolve(hash(uri))));
        assertThat(Files.readSymbolicLink(workDir.resolve("httpzip-2")), is(cacheDir.resolve(hash(uri))));

        assertThat(webServer.getRequestCount(), is(1));
    }

    private FakeFtpServer buildFtpServer() throws Exception {
        FakeFtpServer ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(getRandomPort());

        ftpServer.addUserAccount(new UserAccount("ftepuser", "fteppass", "/data"));

        org.mockftpserver.fake.filesystem.FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data"));
        FileEntry testFile = new FileEntry("/data/testfile.txt");
        byte[] testFileBytes = Files.readAllBytes(Paths.get(FtpDownloaderTest.class.getResource("/testfile.txt").toURI()));
        testFile.setContents(testFileBytes);
        fileSystem.add(testFile);
        ftpServer.setFileSystem(fileSystem);

        return ftpServer;
    }

    private MockWebServer buildWebServer() throws Exception {
        MockWebServer webServer = new MockWebServer();

        InputStream testFileIs = Files.newInputStream(Paths.get(HttpDownloaderTest.class.getResource("/singleFile.zip").toURI()));
        webServer.enqueue(new MockResponse().setBody(new Buffer().readFrom(testFileIs)));

        return webServer;
    }

    private int getRandomPort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    private String hash(Object object) {
        return CachingSymlinkIOManager.HASH_FUNCTION.hashString(object.toString(), Charset.forName("UTF-8")).toString();
    }

}