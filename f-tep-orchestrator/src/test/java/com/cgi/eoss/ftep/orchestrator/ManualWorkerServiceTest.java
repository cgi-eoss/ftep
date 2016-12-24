package com.cgi.eoss.ftep.orchestrator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

public class ManualWorkerServiceTest {

    @Mock
    private ServiceInputOutputManager serviceInputOutputManager;

    private WorkerService workerService;

    @Before
    public void beforeMethod() {
        assumeTrue(Files.isReadable(Paths.get("/var/run/docker.sock")));

        workerService = new ManualWorkerService(serviceInputOutputManager);
    }

    @Test
    public void getWorker() throws Exception {
        Worker worker = workerService.getWorker();
        assertThat(worker.getDockerClient(), is(notNullValue()));
    }

}