package com.cgi.eoss.ftep.io.download;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.persistence.service.RpcServiceFileService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceFileDataService;
import com.cgi.eoss.ftep.rpc.FileStream;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.ServiceContextFilesServiceGrpc;
import com.cgi.eoss.ftep.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.ftep.rpc.catalogue.Databasket;
import com.cgi.eoss.ftep.rpc.catalogue.DatabasketContents;
import com.cgi.eoss.ftep.rpc.catalogue.FtepFile;
import com.cgi.eoss.ftep.rpc.catalogue.FtepFileUri;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
public class FtepDownloaderTest {
    @Mock
    private FtepServerClient ftepServerClient;
    @Mock
    private ServiceDataService serviceDataService;
    @Mock
    private ServiceFileDataService serviceFileDataService;

    private Path targetPath;

    private Path cacheRoot;

    private FileSystem fs;

    private Server server;

    private FtepDownloader dl;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        configureServiceFileDataService();

        this.fs = Jimfs.newFileSystem(Configuration.unix().toBuilder().setAttributeViews("basic", "owner", "posix", "unix").build());
        this.targetPath = this.fs.getPath("/target");
        Files.createDirectories(targetPath);
        this.cacheRoot = this.fs.getPath("/cache");
        Files.createDirectories(cacheRoot);

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(getClass().getName()).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(getClass().getName()).directExecutor();
        RpcServiceFileService rpcServiceFileService = new RpcServiceFileService(serviceDataService, serviceFileDataService);
        inProcessServerBuilder.addService(rpcServiceFileService);
        CatalogueServiceStub catalogueService = new CatalogueServiceStub();
        inProcessServerBuilder.addService(catalogueService);
        server = inProcessServerBuilder.build().start();

        ServiceContextFilesServiceGrpc.ServiceContextFilesServiceBlockingStub serviceContextFilesServiceBlockingStub = ServiceContextFilesServiceGrpc.newBlockingStub(channelBuilder.build());
        CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueServiceBlockingStub = CatalogueServiceGrpc.newBlockingStub(channelBuilder.build());
        when(ftepServerClient.serviceContextFilesServiceBlockingStub()).thenReturn(serviceContextFilesServiceBlockingStub);
        when(ftepServerClient.catalogueServiceBlockingStub()).thenReturn(catalogueServiceBlockingStub);

        this.dl = new FtepDownloader(ftepServerClient, new CachingSymlinkDownloaderFacade(cacheRoot));
        this.dl.postConstruct();
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void testDownloadServiceFiles() throws Exception {
        Path serviceContext = fs.getPath("/target/service1");
        Files.createDirectories(serviceContext);
        URI uri = URI.create("ftep://serviceContext/service1");

        dl.download(serviceContext, uri);

        Set<String> result = Files.walk(serviceContext).filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toSet());
        assertThat(result, is(ImmutableSet.of(
                "/target/service1/Dockerfile",
                "/target/service1/workflow.sh"
        )));
        assertThat(Files.readAllLines(serviceContext.resolve("Dockerfile")), is(ImmutableList.of(
                "FROM hello-world:latest"
        )));
        assertThat(Files.readAllLines(serviceContext.resolve("workflow.sh")), is(ImmutableList.of(
                "#!/usr/bin/env sh",
                "",
                "echo \"This is service1\"",
                "",
                "exit 0"
        )));
        assertThat(Files.isExecutable(serviceContext.resolve("workflow.sh")), is(true));
    }

    @Test
    public void testDownloadDatabasket() throws Exception {
        Path databasketContent = fs.getPath("/target/databasket");
        Files.createDirectories(databasketContent);
        URI uri = URI.create("ftep://databasket/1");

        dl.download(databasketContent, uri);

        Set<String> result = Files.walk(databasketContent, FileVisitOption.FOLLOW_LINKS).filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toSet());
        assertThat(result, is(ImmutableSet.of(
                "/target/databasket/testfile.txt",
                "/target/databasket/filesAndSubdirs.zip/.uri",
                "/target/databasket/filesAndSubdirs.zip/file1",
                "/target/databasket/filesAndSubdirs.zip/file2",
                "/target/databasket/filesAndSubdirs.zip/subdir1/subdir1File1",
                "/target/databasket/filesAndSubdirs.zip/subdir1/subdir1File2",
                "/target/databasket/filesAndSubdirs.zip/subdir2/subdir2File1",
                "/target/databasket/filesAndSubdirs.zip/subdir2/subdir2File2"
        )));
    }

    private void configureServiceFileDataService() throws Exception {
        FtepService service = mock(FtepService.class);
        FtepServiceContextFile dockerfile = new FtepServiceContextFile(service, "Dockerfile");
        dockerfile.setContent(new String(Files.readAllBytes(Paths.get(getClass().getResource("/service1/Dockerfile").toURI()))));
        FtepServiceContextFile workflow = new FtepServiceContextFile(service, "workflow.sh");
        workflow.setContent(new String(Files.readAllBytes(Paths.get(getClass().getResource("/service1/workflow.sh").toURI()))));
        workflow.setExecutable(true);

        when(serviceDataService.getByName("service1")).thenReturn(service);
        when(serviceFileDataService.findByService(service)).thenReturn(ImmutableList.of(dockerfile, workflow));
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