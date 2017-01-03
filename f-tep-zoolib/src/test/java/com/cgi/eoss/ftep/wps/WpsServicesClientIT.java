package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.rest.ApiEntity;
import com.cgi.eoss.ftep.model.rest.ResourceJob;
import com.cgi.eoss.ftep.orchestrator.FtepJsonApi;
import com.cgi.eoss.ftep.orchestrator.JobEnvironmentService;
import com.cgi.eoss.ftep.orchestrator.JobStatusService;
import com.cgi.eoss.ftep.orchestrator.ManualWorkerService;
import com.cgi.eoss.ftep.orchestrator.ProcessorLauncher;
import com.cgi.eoss.ftep.orchestrator.ServiceDataService;
import com.cgi.eoss.ftep.orchestrator.ServiceInputOutputManager;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.grpc.inprocess.InProcessChannelBuilder;
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
 * <p><strong>This uses a real Docker engine to build, run and delete a container!</strong></p>
 */
public class WpsServicesClientIT {
    private static final String RPC_SERVER_NAME = WpsServicesClientIT.class.getName();

    @Mock
    private FtepJsonApi api;

    @Mock
    private ServiceInputOutputManager inputOutputManager;

    @Spy
    @InjectMocks
    private ManualWorkerService workerService;

    @Spy
    @InjectMocks
    private JobStatusService jobStatusService;

    @Spy
    @InjectMocks
    private ServiceDataService serviceDataService;

    @Spy
    @InjectMocks
    private JobEnvironmentService jobEnvironmentService;

    private ProcessorLauncher processorLauncher;

    private FileSystem fs;

    private StandaloneOrchestrator orchestrator;

    private InProcessChannelBuilder channelBuilder;

    private WpsServicesClient wpsServicesClient;

    @Before
    public void setUp() throws Exception {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue(Files.isWritable(Paths.get("/var/run/docker.sock")));
        // TODO Pass in a DOCKER_HOST env var to allow remote docker engine use

        MockitoAnnotations.initMocks(this);

        this.processorLauncher = new ProcessorLauncher(workerService, jobStatusService, serviceDataService, jobEnvironmentService);

        this.fs = Jimfs.newFileSystem(Configuration.unix());

        when(jobEnvironmentService.getBasedir()).thenReturn(fs.getPath("/tmp/ftep_data"));
        Files.createDirectories(fs.getPath("/tmp/ftep_data"));

        StandaloneOrchestrator.resetServices(ImmutableSet.of(processorLauncher));

        orchestrator = new StandaloneOrchestrator(RPC_SERVER_NAME);
        channelBuilder = InProcessChannelBuilder.forName(RPC_SERVER_NAME).directExecutor();
        wpsServicesClient = new WpsServicesClient(channelBuilder);
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

        serviceDataService.registerImageForService("serviceId", "hello-world");

        Multimap<String, String> outputs = wpsServicesClient.launchProcessor(jobId, userId, serviceId, inputs);
        assertThat(outputs, is(notNullValue()));

        List<String> jobConfigLines = Files.readAllLines(fs.getPath("/tmp/ftep_data/Job_jobId/FTEP-WPS-INPUT.properties"));
        assertThat(jobConfigLines, is(ImmutableList.of(
                "inputKey1=inputVal1",
                "inputKey2=inputVal2-1,inputVal2-2"
        )));
    }

}