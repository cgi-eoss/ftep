package com.cgi.eoss.ftep.wps;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PythonStubWpsServiceWriterTest {

    private WpsServiceWriter wpsServiceWriter;

    private FileSystem fs;

    @Before
    public void setUp() {
        this.wpsServiceWriter = new PythonStubWpsServiceWriter();
        this.fs = Jimfs.newFileSystem(Configuration.unix());
    }

    @Test
    public void generateWpsService() throws Exception {
        Path wpsService = fs.getPath("test.py");
        wpsServiceWriter.generateWpsService(ExampleServiceDescriptor.getExampleSvc(), wpsService);

        List<String> generatedLines = Files.readAllLines(wpsService);
        List<String> expectedLines = Files.readAllLines(Paths.get(getClass().getResource("TestService1.py").toURI()));

        assertThat(generatedLines, is(expectedLines));
    }

}