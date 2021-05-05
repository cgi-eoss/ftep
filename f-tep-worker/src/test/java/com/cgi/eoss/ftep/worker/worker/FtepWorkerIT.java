package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.clouds.service.Node;
import com.cgi.eoss.ftep.io.ServiceInputOutputManager;
import com.cgi.eoss.ftep.io.download.DownloaderFacade;
import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.Service;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.JobInputs;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import com.cgi.eoss.ftep.worker.WorkerConfig;
import com.cgi.eoss.ftep.worker.WorkerTestConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {WorkerConfig.class, WorkerTestConfig.class})
@TestPropertySource("classpath:test-worker.properties")
public class FtepWorkerIT {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @MockBean
    private ServiceInputOutputManager ioManager;

    @SpyBean
    private JobEnvironmentService jobEnvironmentService;

    @MockBean // Keep this to avoid instantiating a real Bean which needs a data dir
    private DownloaderFacade downloaderFacade;

    @Autowired
    private InProcessServerBuilder serverBuilder;

    @Autowired
    private ManagedChannelBuilder channelBuilder;

    @Autowired
    private DockerClient dockerClient;

    @Autowired
    private FtepWorkerNodeManager ftepWorkerNodeManager;

    @Autowired
    private FtepWorker worker;

    private Server server;

    private FtepWorkerGrpc.FtepWorkerBlockingStub workerClient;

    private Node node;

    @BeforeClass
    public static void precondition() {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue("Unable to write to Docker socket; disabling docker tests", Files.isWritable(Paths.get("/var/run/docker.sock")));
    }

    @Before
    public void setUp() throws Exception {
        Mockito.when(ioManager.getServiceContext("service1")).thenReturn(Paths.get("src/test/resources/service1").toAbsolutePath());
        Mockito.when(jobEnvironmentService.getBaseDir()).thenReturn(temporaryFolder.getRoot().toPath());

        serverBuilder.addService(worker);
        server = serverBuilder.build().start();

        workerClient = FtepWorkerGrpc.newBlockingStub(channelBuilder.build());

        // Ensure the test base image is available before testing
        dockerClient.pullImageCmd("hello-world:latest").exec(new PullImageResultCallback()).awaitSuccess();

        ListenableFuture<List<Optional<Node>>> provisioningFuture = ftepWorkerNodeManager.provisionNodes(1, FtepWorkerNodeManager.POOLED_WORKER_TAG, jobEnvironmentService.getBaseDir());
        node = provisioningFuture.get().get(0).orElseThrow(() -> new IllegalStateException("Expected to provision a worker node"));
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void testLaunchContainer() throws Exception {
        String tag = UUID.randomUUID().toString();

        // Attach Node manually because the launch request  is not coming through the FtepWorkerDispatcher
        ftepWorkerNodeManager.reattachJobToNode(node, "jobid-1");
        worker.getJobClients().put("jobid-1", dockerClient);

        JobSpec request = JobSpec.newBuilder()
                .setService(Service.newBuilder().setName("service1").setDockerImageTag(tag))
                .setJob(Job.newBuilder().setId("jobid-1"))
                .build();
        try {
            assertThat(dockerClient.listImagesCmd().withImageNameFilter(tag).exec().size(), is(0));
            workerClient.prepareInputs(JobInputs.newBuilder().setJob(request.getJob()).addAllInputs(request.getInputsList()).build());
            workerClient.launchContainer(request);
            assertThat(dockerClient.listImagesCmd().withImageNameFilter(tag).exec().size(), is(1));
        } finally {
            workerClient.cleanUp(request.getJob());
            dockerClient.removeImageCmd(tag).exec();
        }
    }
}
