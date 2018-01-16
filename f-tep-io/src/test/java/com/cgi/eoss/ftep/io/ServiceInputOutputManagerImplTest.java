package com.cgi.eoss.ftep.io;

import com.cgi.eoss.ftep.io.download.CachingSymlinkDownloaderFacade;
import com.cgi.eoss.ftep.io.download.DownloaderFacade;
import com.cgi.eoss.ftep.io.download.FtepDownloader;
import com.cgi.eoss.ftep.io.download.FtpDownloader;
import com.cgi.eoss.ftep.io.download.FtpDownloaderTest;
import com.cgi.eoss.ftep.io.download.HttpDownloader;
import com.cgi.eoss.ftep.io.download.HttpDownloaderTest;
import com.cgi.eoss.ftep.model.DownloaderCredentials;
import com.cgi.eoss.ftep.persistence.service.DownloaderCredentialsDataService;
import com.cgi.eoss.ftep.persistence.service.RpcCredentialsService;
import com.cgi.eoss.ftep.rpc.CredentialsServiceGrpc;
import com.cgi.eoss.ftep.rpc.FileStream;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.ftep.rpc.catalogue.Databasket;
import com.cgi.eoss.ftep.rpc.catalogue.DatabasketContents;
import com.cgi.eoss.ftep.rpc.catalogue.FtepFile;
import com.cgi.eoss.ftep.rpc.catalogue.FtepFileUri;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.hash.Hashing;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.jooq.lambda.Unchecked;
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
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 */
public class ServiceInputOutputManagerImplTest {

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

    private Server server;

    private RpcCredentialsService rpcCredentialsService;

    private CatalogueServiceStub rpcCatalogueService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fs = Jimfs.newFileSystem(Configuration.unix());
        cacheDir = this.fs.getPath("/cache");
        Files.createDirectories(cacheDir);
        workDir = this.fs.getPath("/work");
        Files.createDirectories(workDir);

        when(credentialsDataService.getByHost(any())).thenReturn(
                DownloaderCredentials.basicBuilder().username("ftepuser").password("fteppass").build());

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(getClass().getName()).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(getClass().getName()).directExecutor();

        rpcCredentialsService = new RpcCredentialsService(credentialsDataService);
        rpcCatalogueService = new CatalogueServiceStub();

        inProcessServerBuilder.addService(rpcCredentialsService);
        inProcessServerBuilder.addService(rpcCatalogueService);
        server = inProcessServerBuilder.build().start();

        when(ftepServerClient.credentialsServiceBlockingStub()).thenReturn(CredentialsServiceGrpc.newBlockingStub(channelBuilder.build()));
        when(ftepServerClient.catalogueServiceBlockingStub()).thenReturn(CatalogueServiceGrpc.newBlockingStub(channelBuilder.build()));
        when(ftepServerClient.catalogueServiceStub()).thenReturn(CatalogueServiceGrpc.newStub(channelBuilder.build()));

        ftpServer = buildFtpServer();
        ftpServer.start();
        webServer = buildWebServer();
        webServer.start();

        DownloaderFacade downloaderFacade = new CachingSymlinkDownloaderFacade(cacheDir);
        ioManager = new ServiceInputOutputManagerImpl(ftepServerClient, downloaderFacade);

        new FtpDownloader(downloaderFacade, ftepServerClient).postConstruct();
        new HttpDownloader(downloaderFacade, ftepServerClient, new OkHttpClient.Builder().build()).postConstruct();
        new FtepDownloader(downloaderFacade, ftepServerClient).postConstruct();
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
        Multimap<String, URI> inputs = ImmutableMultimap.of(
                "httpzip", URI.create(httpzipUri),
                "ftptxt", URI.create(ftptxtUri),
                "multi", URI.create(httpzipUri),
                "multi", URI.create(ftptxtUri)
        );

        inputs.asMap().forEach(Unchecked.biConsumer((input, uris) -> ioManager.prepareInput(workDir.resolve(input), uris)));

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

        assertThat(Files.isSymbolicLink(workDir.resolve("ftptxt").resolve("testfile")), is(true));
        assertThat(Files.readSymbolicLink(workDir.resolve("ftptxt").resolve("testfile")), is(cacheDir.resolve(ftptxtUriHash)));
        assertThat(Files.isSymbolicLink(workDir.resolve("httpzip").resolve("singleFile")), is(true));
        assertThat(Files.readSymbolicLink(workDir.resolve("httpzip").resolve("singleFile")), is(cacheDir.resolve(httpzipUriHash)));

        Set<String> result = Files.walk(workDir, FOLLOW_LINKS).map(Path::toString).collect(Collectors.toSet());
        assertThat(result, is(ImmutableSet.of(
                "/work",
                "/work/ftptxt",
                "/work/ftptxt/testfile",
                "/work/ftptxt/testfile/.uri",
                "/work/ftptxt/testfile/testfile.txt",
                "/work/httpzip",
                "/work/httpzip/singleFile",
                "/work/httpzip/singleFile/.uri",
                "/work/httpzip/singleFile/file1",
                "/work/multi",
                "/work/multi/testfile",
                "/work/multi/testfile/.uri",
                "/work/multi/testfile/testfile.txt",
                "/work/multi/singleFile",
                "/work/multi/singleFile/.uri",
                "/work/multi/singleFile/file1"
        )));
        assertThat(Files.readAllLines(workDir.resolve("ftptxt/testfile/.uri")), is(ImmutableList.of(ftptxtUri)));
        assertThat(Files.readAllLines(workDir.resolve("httpzip/singleFile/.uri")), is(ImmutableList.of(httpzipUri)));
    }

    @Test
    public void testCacheReuse() throws Exception {
        URI uri = webServer.url("/singleFile.zip").uri();

        Path firstTarget = workDir.resolve("httpzip-1");
        Path secondTarget = workDir.resolve("httpzip-2");

        ioManager.prepareInput(firstTarget, ImmutableSet.of(uri));
        ioManager.prepareInput(secondTarget, ImmutableSet.of(uri));

        assertThat(Files.readSymbolicLink(workDir.resolve("httpzip-1").resolve("singleFile")), is(cacheDir.resolve(hash(uri))));
        assertThat(Files.readSymbolicLink(workDir.resolve("httpzip-2").resolve("singleFile")), is(cacheDir.resolve(hash(uri))));

        assertThat(webServer.getRequestCount(), is(1));
    }

    @Test
    public void testDatabasketExpansion() throws Exception {
        String httpzipUri = webServer.url("/singleFile.zip").toString();
        String databasketUri = "ftep://databasket/1";
        Multimap<String, URI> inputs = ImmutableMultimap.of(
                "httpzip", URI.create(httpzipUri),
                "databasket", URI.create(databasketUri)
        );

        String databasketUri1 = "ftep://outputProduct/testfile.txt";
        String databasketUri2 = "ftep://refData/filesAndSubdirs.zip";

        inputs.asMap().forEach(Unchecked.biConsumer((input, uris) -> ioManager.prepareInput(workDir.resolve(input), uris)));

        Set<String> cacheResult = Files.walk(cacheDir).map(Path::toString).collect(Collectors.toSet());
        assertThat(cacheResult, containsInAnyOrder(
                "/cache",
                "/cache/" + hash(httpzipUri),
                "/cache/" + hash(httpzipUri) + "/.uri",
                "/cache/" + hash(httpzipUri) + "/file1",
                "/cache/" + hash(databasketUri1),
                "/cache/" + hash(databasketUri1) + "/.uri",
                "/cache/" + hash(databasketUri1) + "/testfile.txt",
                "/cache/" + hash(databasketUri2),
                "/cache/" + hash(databasketUri2) + "/.uri",
                "/cache/" + hash(databasketUri2) + "/file1",
                "/cache/" + hash(databasketUri2) + "/file2",
                "/cache/" + hash(databasketUri2) + "/subdir1",
                "/cache/" + hash(databasketUri2) + "/subdir1/subdir1File1",
                "/cache/" + hash(databasketUri2) + "/subdir1/subdir1File2",
                "/cache/" + hash(databasketUri2) + "/subdir2",
                "/cache/" + hash(databasketUri2) + "/subdir2/subdir2File1",
                "/cache/" + hash(databasketUri2) + "/subdir2/subdir2File2"
        ));

        assertThat(Files.isSymbolicLink(workDir.resolve("databasket").resolve("testfile")), is(true));
        assertThat(Files.readSymbolicLink(workDir.resolve("databasket").resolve("testfile")), is(cacheDir.resolve(hash(databasketUri1))));
        assertThat(Files.readSymbolicLink(workDir.resolve("databasket").resolve("filesAndSubdirs")), is(cacheDir.resolve(hash(databasketUri2))));
        assertThat(Files.isSymbolicLink(workDir.resolve("httpzip").resolve("singleFile")), is(true));
        assertThat(Files.readSymbolicLink(workDir.resolve("httpzip").resolve("singleFile")), is(cacheDir.resolve(hash(httpzipUri))));

        Set<String> result = Files.walk(workDir, FOLLOW_LINKS).map(Path::toString).collect(Collectors.toSet());
        assertThat(result, containsInAnyOrder(
                "/work",
                "/work/httpzip",
                "/work/httpzip/singleFile",
                "/work/httpzip/singleFile/.uri",
                "/work/httpzip/singleFile/file1",
                "/work/databasket",
                "/work/databasket/testfile",
                "/work/databasket/testfile/.uri",
                "/work/databasket/testfile/testfile.txt",
                "/work/databasket/filesAndSubdirs",
                "/work/databasket/filesAndSubdirs/.uri",
                "/work/databasket/filesAndSubdirs/file1",
                "/work/databasket/filesAndSubdirs/file2",
                "/work/databasket/filesAndSubdirs/subdir1",
                "/work/databasket/filesAndSubdirs/subdir1/subdir1File1",
                "/work/databasket/filesAndSubdirs/subdir1/subdir1File2",
                "/work/databasket/filesAndSubdirs/subdir2",
                "/work/databasket/filesAndSubdirs/subdir2/subdir2File1",
                "/work/databasket/filesAndSubdirs/subdir2/subdir2File2"
        ));
        assertThat(Files.readAllLines(workDir.resolve("databasket/testfile/.uri")), is(ImmutableList.of(databasketUri1)));
        assertThat(Files.readAllLines(workDir.resolve("databasket/filesAndSubdirs/.uri")), is(ImmutableList.of(databasketUri2)));
        assertThat(Files.readAllLines(workDir.resolve("httpzip/singleFile/.uri")), is(ImmutableList.of(httpzipUri)));
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
        return Hashing.sha1().hashString(object.toString(), Charset.forName("UTF-8")).toString();
    }

    private static final class CatalogueServiceStub extends CatalogueServiceGrpc.CatalogueServiceImplBase {
        @Override
        public void getDatabasketContents(Databasket request, StreamObserver<DatabasketContents> responseObserver) {
            responseObserver.onNext(DatabasketContents.newBuilder()
                    .addAllFiles(ImmutableList.of(
                            FtepFile.newBuilder()
                                    .setFilename("filesAndSubdirs.zip")
                                    .setUri(FtepFileUri.newBuilder().setUri("ftep://refData/filesAndSubdirs.zip").build())
                                    .build(),
                            FtepFile.newBuilder()
                                    .setFilename("testfile.txt")
                                    .setUri(FtepFileUri.newBuilder().setUri("ftep://outputProduct/testfile.txt").build())
                                    .build()
                    ))
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void downloadFtepFile(FtepFileUri request, StreamObserver<FileStream> responseObserver) {
            try {
                URI uri = URI.create(request.getUri());
                Path fileContent = Paths.get(getClass().getResource(uri.getPath()).toURI());

                // First message is the metadata
                FileStream.FileMeta fileMeta = FileStream.FileMeta.newBuilder()
                        .setFilename(fileContent.getFileName().toString())
                        .setSize(Files.size(fileContent))
                        .build();
                responseObserver.onNext(FileStream.newBuilder().setMeta(fileMeta).build());

                // Then the content
                responseObserver.onNext(FileStream.newBuilder().setChunk(FileStream.Chunk.newBuilder()
                        .setPosition(0)
                        .setData(ByteString.copyFrom(Files.readAllBytes(fileContent)))
                        .build()).build());

                responseObserver.onCompleted();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}