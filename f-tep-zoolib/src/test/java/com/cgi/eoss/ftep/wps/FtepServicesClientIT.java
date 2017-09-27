package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.clouds.local.LocalNodeFactory;
import com.cgi.eoss.ftep.clouds.service.NodeFactory;
import com.cgi.eoss.ftep.costing.CostingService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.orchestrator.service.FtepGuiServiceManager;
import com.cgi.eoss.ftep.orchestrator.service.FtepServiceLauncher;
import com.cgi.eoss.ftep.orchestrator.service.WorkerFactory;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.cgi.eoss.ftep.io.ServiceInputOutputManager;
import com.cgi.eoss.ftep.worker.worker.FtepWorker;
import com.cgi.eoss.ftep.worker.worker.JobEnvironmentService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.MoreFiles;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import shadow.dockerjava.com.github.dockerjava.api.DockerClient;
import shadow.dockerjava.com.github.dockerjava.core.DefaultDockerClientConfig;
import shadow.dockerjava.com.github.dockerjava.core.DockerClientBuilder;
import shadow.dockerjava.com.github.dockerjava.core.DockerClientConfig;
import shadow.dockerjava.com.github.dockerjava.core.RemoteApiVersion;
import shadow.dockerjava.com.github.dockerjava.core.command.PullImageResultCallback;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>Integration test for launching WPS services.</p>
 * <p><strong>This uses a real Docker engine to build and run a container!</strong></p>
 */
public class FtepServicesClientIT {
    private static final String RPC_SERVER_NAME = FtepServicesClientIT.class.getName();
    private static final String SERVICE_NAME = "service1";
    private static final String APPLICATION_NAME = "service2";
    private static final String TEST_CONTAINER_IMAGE = "alpine:latest";

    @Mock
    private FtepGuiServiceManager guiService;

    @Mock
    private JobDataService jobDataService;

    @Mock
    private CatalogueService catalogueService;

    @Mock
    private CostingService costingService;

    private Path workspace;
    private Path ingestedOutputsDir;

    private FtepServicesClient ftepServicesClient;

    private Server server;

    @BeforeClass
    public static void precondition() {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue("Unable to write to Docker socket; disabling docker tests", Files.isWritable(Paths.get("/var/run/docker.sock")));
        // TODO Pass in a DOCKER_HOST env var to allow remote docker engine use
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        workspace = Files.createTempDirectory(Paths.get("target"), FtepServicesClientIT.class.getSimpleName());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                MoreFiles.deleteRecursively(workspace);
            } catch (IOException ignored) {
            }
        }));
        ingestedOutputsDir = workspace.resolve("ingestedOutputsDir");
        Files.createDirectories(ingestedOutputsDir);

        when(catalogueService.provisionNewOutputProduct(any(), any()))
                .thenAnswer(invocation -> ingestedOutputsDir.resolve((String) invocation.getArgument(1)));
        when(catalogueService.ingestOutputProduct(any(), any())).thenAnswer(invocation -> {
            Path outputPath = (Path) invocation.getArgument(1);
            FtepFile ftepFile = new FtepFile(URI.create("ftep://output/" + ingestedOutputsDir.relativize(outputPath)), UUID.randomUUID());
            ftepFile.setFilename(ingestedOutputsDir.relativize(outputPath).toString());
            return ftepFile;
        });

        JobEnvironmentService jobEnvironmentService = spy(new JobEnvironmentService(workspace));
        ServiceInputOutputManager ioManager = mock(ServiceInputOutputManager.class);
        Mockito.when(ioManager.getServiceContext(SERVICE_NAME)).thenReturn(Paths.get("src/test/resources/service1").toAbsolutePath());
        Mockito.when(ioManager.getServiceContext(APPLICATION_NAME)).thenReturn(Paths.get("src/test/resources/service2").toAbsolutePath());

        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(RemoteApiVersion.VERSION_1_19)
                .withDockerHost("unix:///var/run/docker.sock")
                .build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build();
        NodeFactory nodeFactory = new LocalNodeFactory(-1, "unix:///var/run/docker.sock");

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(RPC_SERVER_NAME).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(RPC_SERVER_NAME).directExecutor();

        WorkerFactory workerFactory = mock(WorkerFactory.class);
        FtepSecurityService securityService = mock(FtepSecurityService.class);

        FtepServiceLauncher ftepServiceLauncher = new FtepServiceLauncher(workerFactory, jobDataService, guiService, catalogueService, costingService, securityService);
        FtepWorker ftepWorker = new FtepWorker(nodeFactory, jobEnvironmentService, ioManager);

        when(workerFactory.getWorker(any())).thenReturn(FtepWorkerGrpc.newBlockingStub(channelBuilder.build()));

        inProcessServerBuilder.addService(ftepServiceLauncher);
        inProcessServerBuilder.addService(ftepWorker);

        server = inProcessServerBuilder.build().start();

        ftepServicesClient = new FtepServicesClient(channelBuilder);

        // Ensure the test image is available before testing
        dockerClient.pullImageCmd(TEST_CONTAINER_IMAGE).exec(new PullImageResultCallback()).awaitSuccess();
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void launchApplication() throws Exception {
        FtepService service = mock(FtepService.class);
        FtepServiceDescriptor serviceDescriptor = mock(FtepServiceDescriptor.class);
        User user = mock(User.class);
        when(user.getName()).thenReturn("ftep-user");
        Wallet wallet = mock(Wallet.class);
        when(user.getWallet()).thenReturn(wallet);
        when(wallet.getBalance()).thenReturn(100);

        when(service.getName()).thenReturn(APPLICATION_NAME);
        when(service.getDockerTag()).thenReturn("ftep/testservice2");
        when(service.getType()).thenReturn(FtepService.Type.APPLICATION); // Trigger ingestion of all outputs
        when(service.getServiceDescriptor()).thenReturn(serviceDescriptor);
        when(jobDataService.buildNew(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            JobConfig config = new JobConfig(user, service);
            config.setLabel(Strings.isNullOrEmpty(invocation.getArgument(3)) ? null : invocation.getArgument(3));
            config.setInputs(invocation.getArgument(4));
            Job job = new Job(config, invocation.getArgument(0), user);
            job.setId(1L);
            return job;
        });

        String jobId = UUID.randomUUID().toString();
        String userId = "userId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("inputKey1", "inputVal1")
                .putAll("inputKey2", ImmutableList.of("inputVal2-1", "inputVal2-2"))
                .build();

        when(costingService.estimateJobCost(any())).thenReturn(20);

        Multimap<String, String> outputs = ftepServicesClient.launchService(userId, SERVICE_NAME, jobId, inputs);
        assertThat(outputs, is(notNullValue()));
        assertThat(outputs.values(), containsInAnyOrder(
                "ftep://output/output_file_1",
                "ftep://output/A_plot.zip",
                "ftep://output/A_plot.qpj",
                "ftep://output/A_point.qpj"
        ));

        List<String> jobConfigLines = Files.readAllLines(workspace.resolve("Job_" + jobId + "/FTEP-WPS-INPUT.properties"));
        assertThat(jobConfigLines, is(ImmutableList.of(
                "inputKey1=\"inputVal1\"",
                "inputKey2=\"inputVal2-1,inputVal2-2\""
        )));

        List<String> outputFileLines = Files.readAllLines(ingestedOutputsDir.resolve("output_file_1"));
        assertThat(outputFileLines, is(ImmutableList.of("INPUT PARAM: inputVal1")));

        verify(costingService).chargeForJob(eq(wallet), any());
    }

    @Test
    public void launchProcessor() throws Exception {
        FtepService service = mock(FtepService.class);
        FtepServiceDescriptor serviceDescriptor = mock(FtepServiceDescriptor.class);
        User user = mock(User.class);
        when(user.getName()).thenReturn("ftep-user");
        Wallet wallet = mock(Wallet.class);
        when(user.getWallet()).thenReturn(wallet);
        when(wallet.getBalance()).thenReturn(100);

        when(service.getName()).thenReturn(SERVICE_NAME);
        when(service.getDockerTag()).thenReturn("ftep/testservice1");
        when(service.getType()).thenReturn(FtepService.Type.PROCESSOR);
        when(service.getServiceDescriptor()).thenReturn(serviceDescriptor);
        when(serviceDescriptor.getDataOutputs()).thenReturn(ImmutableList.of(
                FtepServiceDescriptor.Parameter.builder().id("output").build()
        ));
        when(jobDataService.buildNew(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            JobConfig config = new JobConfig(user, service);
            config.setLabel(Strings.isNullOrEmpty(invocation.getArgument(3)) ? null : invocation.getArgument(3));
            config.setInputs(invocation.getArgument(4));
            Job job = new Job(config, invocation.getArgument(0), user);
            job.setId(1L);
            return job;
        });

        String jobId = UUID.randomUUID().toString();
        String userId = "userId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("inputKey1", "inputVal1")
                .putAll("inputKey2", ImmutableList.of("inputVal2-1", "inputVal2-2"))
                .build();

        when(costingService.estimateJobCost(any())).thenReturn(20);

        Multimap<String, String> outputs = ftepServicesClient.launchService(userId, SERVICE_NAME, jobId, inputs);
        assertThat(outputs, is(notNullValue()));
        assertThat(outputs.get("output"), containsInAnyOrder("ftep://output/output_file_1"));

        List<String> jobConfigLines = Files.readAllLines(workspace.resolve("Job_" + jobId + "/FTEP-WPS-INPUT.properties"));
        assertThat(jobConfigLines, is(ImmutableList.of(
                "inputKey1=\"inputVal1\"",
                "inputKey2=\"inputVal2-1,inputVal2-2\""
        )));

        List<String> outputFileLines = Files.readAllLines(ingestedOutputsDir.resolve("output_file_1"));
        assertThat(outputFileLines, is(ImmutableList.of("INPUT PARAM: inputVal1")));

        verify(costingService).chargeForJob(eq(wallet), any());
    }

}
