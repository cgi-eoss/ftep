package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
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


public class FtepServiceDescriptorYamlHandlerTest {

    private FtepServiceDescriptorYamlHandler handler;

    private FileSystem fs;

    @Before
    public void setUp() {
        this.handler = new FtepServiceDescriptorYamlHandler();
        this.fs = Jimfs.newFileSystem(Configuration.unix());
    }

    @Test
    public void readFile() throws Exception {
        FtepServiceDescriptor svc = handler.readFile(Paths.get(getClass().getResource("TestService1.yaml").toURI()));
        assertThat(svc, is(ExampleServiceDescriptor.getExampleSvc()));
    }

    @Test
    public void writeFile() throws Exception {
        Path yaml = fs.getPath("test.yaml");
        FtepServiceDescriptor svc = ExampleServiceDescriptor.getExampleSvc();
        handler.writeFile(svc, yaml);

        List<String> generatedLines = Files.readAllLines(yaml);
        List<String> expectedLines = Files.readAllLines(Paths.get(getClass().getResource("TestService1.yaml").toURI()));

        assertThat(generatedLines, is(expectedLines));
    }

}