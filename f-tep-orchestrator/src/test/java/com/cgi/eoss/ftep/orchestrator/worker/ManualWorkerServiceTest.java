package com.cgi.eoss.ftep.orchestrator.worker;

import com.cgi.eoss.ftep.orchestrator.io.ServiceInputOutputManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

public class ManualWorkerServiceTest {

    @Mock
    private JobEnvironmentService jobEnvironmentService;

    @Mock
    private ServiceInputOutputManager serviceInputOutputManager;

    private WorkerService workerService;

    @Before
    public void setUp() {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue(Files.isWritable(Paths.get("/var/run/docker.sock")));

        MockitoAnnotations.initMocks(this);

        workerService = new ManualWorkerService(jobEnvironmentService, serviceInputOutputManager);
    }

    @Test
    public void getWorker() throws Exception {
        Worker worker = workerService.getWorker();
        assertThat(worker.getDockerClient(), is(notNullValue()));
    }

}