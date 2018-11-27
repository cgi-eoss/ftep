package com.cgi.eoss.ftep.server;

import com.cgi.eoss.ftep.catalogue.geoserver.GeoserverService;
import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.clouds.local.LocalNodeFactory;
import com.cgi.eoss.ftep.clouds.service.NodeFactory;
import com.cgi.eoss.ftep.io.ServiceInputOutputManager;
import com.cgi.eoss.ftep.io.download.DownloaderFacade;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.orchestrator.service.FtepJobLauncher;
import com.cgi.eoss.ftep.orchestrator.service.WorkerFactory;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.cgi.eoss.ftep.rpc.FtepJobLauncherGrpc;
import com.cgi.eoss.ftep.rpc.FtepJobResponse;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.worker.FtepWorkerApplication;
import com.cgi.eoss.ftep.worker.WorkerConfig;
import com.cgi.eoss.ftep.worker.worker.FtepWorker;
import com.cgi.eoss.ftep.worker.worker.FtepWorkerNodeManager;
import com.cgi.eoss.ftep.worker.worker.JobEnvironmentService;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.io.MoreFiles;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        FtepServerApplicationTest.FtepServerApplicationTestConfig.class
})
@Configuration
@Import({
        FtepWorkerApplication.class,
        WorkerConfig.class
})
@TestPropertySource
public class FtepServerApplicationTest {
    private static final String SERVICE_NAME = "service1";
    private static final User TESTUSER = new User("testuser");
    private static final UUID FTEP_JOB_ID = UUID.randomUUID();

    @Autowired
    private InProcessServerBuilder serverBuilder;

    @Autowired
    private ManagedChannelBuilder channelBuilder;

    @Autowired
    private FtepJobLauncher ftepJobLauncher;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private FtepWorkerNodeManager nodeManager;

    @MockBean
    private WorkerFactory workerFactory;

    @MockBean
    private ServiceInputOutputManager ioManager;

    @MockBean // Keep this to avoid instantiating a real Bean which needs a data dir
    private DownloaderFacade downloaderFacade;

    @MockBean
    private GeoserverService geoserverService;

    @MockBean
    private RestoService restoService;

    @Autowired
    private Path workspace;

    private Path ingestedOutputsDir;

    private Server server;

    @BeforeClass
    public static void precondition() {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue("Unable to write to Docker socket; disabling docker tests", Files.isWritable(Paths.get("/var/run/docker.sock")));
    }

    @Configuration
    @Import({
            RpcTestConfig.class,
            FtepServerApplication.class
    })
    public static class FtepServerApplicationTestConfig {
        @Bean
        public Path workspace() throws IOException {
            Path workspace = Files.createTempDirectory(Paths.get("target"), FtepServerApplicationTest.class.getSimpleName());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    MoreFiles.deleteRecursively(workspace);
                } catch (IOException ignored) {
                }
            }));
            return workspace;
        }

        @Bean
        public Path outputProductBasedir(Path workspace) throws IOException {
            return Files.createDirectory(workspace.resolve("outputProducts"));
        }

        @Bean
        public Path referenceDataBasedir(Path workspace) throws IOException {
            return Files.createDirectory(workspace.resolve("refData"));
        }
    }

    @Before
    public void setUp() throws Exception {
        ingestedOutputsDir = workspace.resolve("ingestedOutputsDir");
        Files.createDirectories(ingestedOutputsDir);

        NodeFactory nodeFactory = new LocalNodeFactory(-1, "unix:///var/run/docker.sock");

        when(ioManager.getServiceContext(SERVICE_NAME)).thenReturn(Paths.get("src/test/resources/service1").toAbsolutePath());

        serverBuilder.addService(ftepJobLauncher);
        serverBuilder.addService(new FtepWorker(nodeManager, new JobEnvironmentService(workspace), ioManager, 1));
        server = serverBuilder.build().start();

        when(workerFactory.getWorker(any())).thenReturn(FtepWorkerGrpc.newBlockingStub(channelBuilder.build()));

        User owner = userDataService.save(TESTUSER);

        FtepService testservice = new FtepService(SERVICE_NAME, owner, "test/testservice1");
        FtepServiceDescriptor serviceDescriptor = new FtepServiceDescriptor();
        serviceDescriptor.setDataOutputs(ImmutableList.of(FtepServiceDescriptor.Parameter.builder().id("output").build()));
        testservice.setServiceDescriptor(serviceDescriptor);
        serviceDataService.save(testservice);

        when(restoService.ingestOutputProduct(any())).thenReturn(FTEP_JOB_ID);
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void test() {
        FtepJobLauncherGrpc.FtepJobLauncherBlockingStub jobLauncher = FtepJobLauncherGrpc.newBlockingStub(channelBuilder.build());
        Iterator<FtepJobResponse> jobResponseIterator = jobLauncher.launchService(FtepServiceParams.newBuilder()
                .setServiceId(SERVICE_NAME)
                .setUserId(TESTUSER.getName())
                .setJobId(String.valueOf(FTEP_JOB_ID))
                .addAllInputs(GrpcUtil.mapToParams(
                        ImmutableMultimap.<String, String>builder()
                                .put("inputKey1", "inputVal1")
                                .putAll("inputKey2", ImmutableList.of("inputVal2-1", "inputVal2-2"))
                                .build()
                ))
                .build());

        Job job = jobResponseIterator.next().getJob();
        assertThat(job, is(notNullValue()));
        FtepJobResponse.JobOutputs jobOutputs = jobResponseIterator.next().getJobOutputs();
        assertThat(jobOutputs, is(notNullValue()));
    }
}
