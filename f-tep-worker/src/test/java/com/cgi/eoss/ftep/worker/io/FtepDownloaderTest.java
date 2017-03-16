package com.cgi.eoss.ftep.worker.io;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.persistence.service.RpcServiceFileService;
import com.cgi.eoss.ftep.persistence.service.ServiceFileDataService;
import com.cgi.eoss.ftep.rpc.ServiceContextFilesServiceGrpc;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.ServerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.nio.file.FileSystem;
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
    private ServiceFileDataService serviceFileDataService;

    private Path targetPath;

    private FileSystem fs;

    private ServerImpl server;

    private FtepDownloader dl;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        configureServiceFileDataService();

        this.fs = Jimfs.newFileSystem(Configuration.unix());
        this.targetPath = this.fs.getPath("/target");
        Files.createDirectories(targetPath);

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(getClass().getName()).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(getClass().getName()).directExecutor();
        RpcServiceFileService rpcServiceFileService = new RpcServiceFileService(serviceFileDataService);
        inProcessServerBuilder.addService(rpcServiceFileService);
        server = inProcessServerBuilder.build().start();

        this.dl = new FtepDownloader(ServiceContextFilesServiceGrpc.newBlockingStub(channelBuilder.build()));
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
    }

    private void configureServiceFileDataService() throws Exception {
        FtepService service = mock(FtepService.class);
        FtepServiceContextFile dockerfile = new FtepServiceContextFile(service, "Dockerfile");
        dockerfile.setContent(new String(Files.readAllBytes(Paths.get(getClass().getResource("/service1/Dockerfile").toURI()))));
        FtepServiceContextFile workflow = new FtepServiceContextFile(service, "workflow.sh");
        workflow.setContent(new String(Files.readAllBytes(Paths.get(getClass().getResource("/service1/workflow.sh").toURI()))));

        when(serviceFileDataService.findByService("service1")).thenReturn(ImmutableList.of(dockerfile, workflow));
    }

}