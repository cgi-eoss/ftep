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

public class ZcfgWriterImplTest {

    private ZcfgWriter zcfgWriter;

    private FileSystem fs;

    @Before
    public void setUp() {
        this.zcfgWriter = new ZcfgWriterImpl();
        this.fs = Jimfs.newFileSystem(Configuration.unix());
    }

    @Test
    public void generateZcfg() throws Exception {
        Path zcfg = fs.getPath("test.zcfg");
        zcfgWriter.generateZcfg(ExampleServiceDescriptor.getExampleSvc(), zcfg);

        List<String> generatedLines = Files.readAllLines(zcfg);
        List<String> expectedLines = Files.readAllLines(Paths.get(getClass().getResource("TestService1.zcfg").toURI()));

        assertThat(generatedLines, is(expectedLines));
    }

}