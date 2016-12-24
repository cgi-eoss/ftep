package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.StandaloneWpsStub;
import com.cgi.eoss.ftep.model.rest.ApiEntity;
import com.cgi.eoss.ftep.model.rest.ResourceJob;
import com.cgi.eoss.ftep.orchestrator.FtepJsonApi;
import com.cgi.eoss.ftep.orchestrator.JobEnvironmentService;
import com.cgi.eoss.ftep.orchestrator.JobStatusService;
import com.cgi.eoss.ftep.orchestrator.ManualWorkerService;
import com.cgi.eoss.ftep.orchestrator.ServiceInputOutputManager;
import com.cgi.eoss.ftep.orchestrator.Worker;
import com.cgi.eoss.ftep.orchestrator.ApplicationLauncher;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.nio.file.FileSystem;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WpsStubLauncherIT {

    @Mock
    private Worker worker;

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
    private JobEnvironmentService jobEnvironmentService;

    private ApplicationLauncher applicationLauncher;

    private FileSystem fs;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.applicationLauncher = new ApplicationLauncher(workerService, jobStatusService, jobEnvironmentService);

        this.fs = Jimfs.newFileSystem(Configuration.unix());

        when(jobEnvironmentService.getBasedir()).thenReturn(fs.getPath("/"));

        when(workerService.getWorker()).thenReturn(worker);
        StandaloneOrchestrator.resetServices(ImmutableSet.of(applicationLauncher));
    }

    @Test
    public void testStandaloneWpsStub() throws Exception {
        ResourceJob resourceJob = mock(ResourceJob.class);
        when(api.insert(any())).thenReturn(ApiEntity.<ResourceJob>builder()
                .resource(resourceJob)
                .resourceEndpoint("http://example.com/api/v1.0/jobs")
                .resourceId("1")
                .build());

        when(worker.launchDockerContainer(any())).thenReturn("test-container-id");
        when(worker.getContainerPortBindings("test-container-id")).thenReturn(ImmutableMultimap.<String, String>builder()
                .put("8080/tcp", "localhost:54321")
                .build());

        HashMap conf = Maps.newHashMap();
        HashMap inputs = Maps.newHashMap();
        HashMap outputs = Maps.newHashMap();

        int ret = StandaloneWpsStub.StandaloneWpsStub(conf, inputs, outputs);
        assertThat(ret, is(3));
    }

}
