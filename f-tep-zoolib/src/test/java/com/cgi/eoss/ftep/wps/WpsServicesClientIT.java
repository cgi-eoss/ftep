package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.orchestrator.FtepServiceLauncher;
import com.cgi.eoss.ftep.orchestrator.io.ServiceInputOutputManager;
import com.cgi.eoss.ftep.orchestrator.worker.JobEnvironmentService;
import com.cgi.eoss.ftep.orchestrator.worker.ManualWorkerService;
import com.cgi.eoss.ftep.orchestrator.worker.WorkerFactory;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * <p>Integration test for launching WPS services.</p>
 * <p><strong>This uses a real Docker engine to build and run a container!</strong></p>
 */
public class WpsServicesClientIT {
    private static final String RPC_SERVER_NAME = WpsServicesClientIT.class.getName();
    private static final String TEST_CONTAINER_IMAGE = "hello-world:latest";

    @Mock
    private JobDataService jobDataService;

    private FileSystem fs;

    private FtepServicesClient ftepServicesClient;

    @Before
    public void setUp() throws Exception {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue("Unable to write to Docker socket; disabling docker tests", Files.isWritable(Paths.get("/var/run/docker.sock")));
        // TODO Pass in a DOCKER_HOST env var to allow remote docker engine use

        MockitoAnnotations.initMocks(this);

        this.fs = Jimfs.newFileSystem(Configuration.unix());
        Files.createDirectories(fs.getPath("/tmp/ftep_data"));

        JobEnvironmentService jobEnvironmentService = spy(new JobEnvironmentService(fs.getPath("/tmp/ftep_data")));
        ServiceInputOutputManager ioManager = mock(ServiceInputOutputManager.class);
        ManualWorkerService manualWorkerService = new ManualWorkerService(jobEnvironmentService, ioManager);

        WorkerFactory workerFactory = new WorkerFactory(manualWorkerService);

        FtepServiceLauncher ftepServiceLauncher = new FtepServiceLauncher(workerFactory, jobDataService);

        StandaloneOrchestrator.resetServices(ImmutableSet.of(ftepServiceLauncher));
        StandaloneOrchestrator orchestrator = new StandaloneOrchestrator(RPC_SERVER_NAME);
        ftepServicesClient = new FtepServicesClient(orchestrator.getChannelBuilder());

        // Ensure the test image is available before testing
        manualWorkerService.getWorker().getDockerClient().pullImageCmd(TEST_CONTAINER_IMAGE).exec(new PullImageResultCallback()).awaitSuccess();
    }

    @Test
    public void launchProcessor() throws Exception {
        FtepService service = mock(FtepService.class);
        User user = mock(User.class);

        when(service.getDescription()).thenReturn(TEST_CONTAINER_IMAGE);
        when(jobDataService.buildNew(any(), any(), any(), any())).thenAnswer(invocation -> {
            JobConfig config = new JobConfig(user, service);
            config.setInputs(invocation.getArgument(3));
            return new Job(config, invocation.getArgument(0), user);
        });

        String jobId = "jobId";
        String userId = "userId";
        String serviceId = "serviceId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("inputKey1", "inputVal1")
                .putAll("inputKey2", ImmutableList.of("inputVal2-1", "inputVal2-2"))
                .build();

        Multimap<String, String> outputs = ftepServicesClient.launchService(jobId, userId, serviceId, inputs);
        assertThat(outputs, is(notNullValue()));

        List<String> jobConfigLines = Files.readAllLines(fs.getPath("/tmp/ftep_data/Job_jobId/FTEP-WPS-INPUT.properties"));
        assertThat(jobConfigLines, is(ImmutableList.of(
                "inputKey1=inputVal1",
                "inputKey2=inputVal2-1,inputVal2-2"
        )));
    }

}
