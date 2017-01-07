package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.enums.ServiceType;
import com.cgi.eoss.ftep.model.rest.ApiEntity;
import com.cgi.eoss.ftep.model.rest.ResourceJob;
import com.cgi.eoss.ftep.orchestrator.FtepJsonApi;
import com.cgi.eoss.ftep.orchestrator.JobEnvironmentService;
import com.cgi.eoss.ftep.orchestrator.JobStatusService;
import com.cgi.eoss.ftep.orchestrator.ManualWorkerService;
import com.cgi.eoss.ftep.orchestrator.ProcessorLauncher;
import com.cgi.eoss.ftep.orchestrator.ServiceDataService;
import com.cgi.eoss.ftep.orchestrator.ServiceInputOutputManager;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * <p>Integration test for launching WPS services.</p>
 * <p><strong>This uses a real Docker engine to build and run a container!</strong></p>
 */
public class WpsServicesClientIT {
    private static final String RPC_SERVER_NAME = WpsServicesClientIT.class.getName();
    public static final String TEST_CONTAINER_IMAGE = "hello-world:latest";

    @Mock
    private FtepJsonApi api;

    @Mock
    private ServiceInputOutputManager inputOutputManager;

    @Spy
    private JobEnvironmentService jobEnvironmentService;

    @Spy
    @InjectMocks
    private ManualWorkerService workerService;

    @Spy
    @InjectMocks
    private JobStatusService jobStatusService;

    @Spy
    @InjectMocks
    private ServiceDataService serviceDataService;

    private FileSystem fs;

    private WpsServicesClient wpsServicesClient;

    @Before
    public void setUp() throws Exception {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue("Unable to write to Docker socket; disabling docker tests", Files.isWritable(Paths.get("/var/run/docker.sock")));
        // TODO Pass in a DOCKER_HOST env var to allow remote docker engine use

        MockitoAnnotations.initMocks(this);

        ProcessorLauncher processorLauncher = new ProcessorLauncher(workerService, jobStatusService, serviceDataService);

        this.fs = Jimfs.newFileSystem(Configuration.unix());

        when(jobEnvironmentService.getBasedir()).thenReturn(fs.getPath("/tmp/ftep_data"));
        Files.createDirectories(fs.getPath("/tmp/ftep_data"));

        StandaloneOrchestrator.resetServices(ImmutableSet.of(processorLauncher));
        StandaloneOrchestrator orchestrator = new StandaloneOrchestrator(RPC_SERVER_NAME);
        wpsServicesClient = new WpsServicesClient(orchestrator.getChannelBuilder());

        serviceDataService.registerService("serviceId", TEST_CONTAINER_IMAGE, ServiceType.PROCESSOR);

        // Ensure the test image is available before testing
        workerService.getWorker().getDockerClient().pullImageCmd(TEST_CONTAINER_IMAGE).exec(new PullImageResultCallback()).awaitSuccess();
    }

    @Test
    public void launchProcessor() throws Exception {
        when(api.insert(any())).thenAnswer(invocation -> ApiEntity.<ResourceJob>builder()
                .resource(invocation.getArgument(0))
                .resourceEndpoint("http://example.com/api/v1.0/jobs")
                .resourceId("1")
                .build());

        String jobId = "jobId";
        String userId = "userId";
        String serviceId = "serviceId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("inputKey1", "inputVal1")
                .putAll("inputKey2", ImmutableList.of("inputVal2-1", "inputVal2-2"))
                .build();

        Multimap<String, String> outputs = wpsServicesClient.launchProcessor(jobId, userId, serviceId, inputs);
        assertThat(outputs, is(notNullValue()));

        List<String> jobConfigLines = Files.readAllLines(fs.getPath("/tmp/ftep_data/Job_jobId/FTEP-WPS-INPUT.properties"));
        assertThat(jobConfigLines, is(ImmutableList.of(
                "inputKey1=inputVal1",
                "inputKey2=inputVal2-1,inputVal2-2"
        )));
    }

}
