package com.cgi.eoss.ftep.orchestrator.worker;

import com.cgi.eoss.ftep.orchestrator.io.ServiceInputOutputManager;
import com.github.dockerjava.api.DockerClient;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class WorkerTest {

    private Path cachePath;

    @Mock
    private DockerClient dockerClient;
    @Mock
    private JobEnvironmentService jobEnvironmentService;
    @Mock
    private ServiceInputOutputManager inputOutputManager;

    @InjectMocks
    private Worker worker;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        cachePath = fs.getPath("/data/cache");
        Files.createDirectories(cachePath);
    }

    @Test
    public void prepareInputs() throws Exception {
        worker.prepareInputs(ImmutableMultimap.of(
                "input1", "foo",
                "input2", "http://test",
                "input3", "foo/bar"
        ), cachePath);

        // Verifies that only the recognisable URL is processed by the inputOutputManager
        verify(inputOutputManager).prepareInput(cachePath.resolve("input2"), URI.create("http://test"));
        verifyNoMoreInteractions(inputOutputManager);
    }

}