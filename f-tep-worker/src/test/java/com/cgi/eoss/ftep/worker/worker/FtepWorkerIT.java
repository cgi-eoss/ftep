package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.ftep.worker.WorkerConfig;
import com.cgi.eoss.ftep.worker.WorkerTestConfig;
import com.cgi.eoss.ftep.worker.io.ServiceInputOutputManager;
import shadow.dockerjava.com.github.dockerjava.api.DockerClient;
import shadow.dockerjava.com.github.dockerjava.core.command.PullImageResultCallback;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.internal.ServerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {WorkerConfig.class, WorkerTestConfig.class})
@TestPropertySource("classpath:test-worker.properties")
public class FtepWorkerIT {

    @MockBean
    private ServiceInputOutputManager ioManager;

    @Autowired
    private InProcessServerBuilder serverBuilder;

    @Autowired
    private ManagedChannelBuilder channelBuilder;

    @Autowired
    private DockerClient dockerClient;

    @Autowired
    private FtepWorker worker;

    private ServerImpl server;

    private FtepWorkerGrpc.FtepWorkerBlockingStub workerClient;

    @BeforeClass
    public static void precondition() {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue("Unable to write to Docker socket; disabling docker tests", Files.isWritable(Paths.get("/var/run/docker.sock")));
        // TODO Pass in a DOCKER_HOST env var to allow remote docker engine use
    }

    @Before
    public void setUp() throws IOException {
        serverBuilder.addService(worker);
        server = serverBuilder.build().start();

        workerClient = FtepWorkerGrpc.newBlockingStub(channelBuilder.build());

        // Ensure the test base image is available before testing
        dockerClient.pullImageCmd("hello-world:latest").exec(new PullImageResultCallback()).awaitSuccess();
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void testLaunchContainer() throws Exception {
        Mockito.when(ioManager.getServiceContext("service1")).thenReturn(Paths.get("src/test/resources/service1").toAbsolutePath());

        String tag = UUID.randomUUID().toString();

        worker.getJobClients().put("jobid-1", dockerClient);

        JobDockerConfig request = JobDockerConfig.newBuilder()
                .setServiceName("service1")
                .setJob(Job.newBuilder().setId("jobid-1"))
                .setDockerImage(tag)
                .build();
        try {
            assertThat(dockerClient.listImagesCmd().withImageNameFilter(tag).exec().size(), is(0));
            workerClient.launchContainer(request);
            assertThat(dockerClient.listImagesCmd().withImageNameFilter(tag).exec().size(), is(1));
        } finally {
            dockerClient.removeImageCmd(tag).exec();
        }
    }

}
