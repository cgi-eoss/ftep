package com.cgi.eoss.ftep.io;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ZipHandlerTest {

    private final String targetDir = "/target";

    private FileSystem fs;

    @Before
    public void setUp() throws Exception {
        this.fs = Jimfs.newFileSystem(Configuration.unix());
        Files.createDirectories(this.fs.getPath(targetDir));
    }

    @Test
    public void unzipFiles() throws Exception {
        Path zip = Paths.get(ZipHandlerTest.class.getResource("/files.zip").toURI());
        Path target = this.fs.getPath(targetDir);

        ZipHandler.unzip(zip, target);

        List<String> result = Files.walk(target).map(Path::toString).sorted().collect(Collectors.toList());
        assertThat(result, is(ImmutableList.of(
                "/target",
                "/target/file1",
                "/target/file2"
        )));
    }

    @Test
    public void unzipSubdirs() throws Exception {
        Path zip = Paths.get(ZipHandlerTest.class.getResource("/subdirs.zip").toURI());
        Path target = this.fs.getPath(targetDir);

        ZipHandler.unzip(zip, target);

        List<String> result = Files.walk(target).map(Path::toString).sorted().collect(Collectors.toList());
        assertThat(result, is(ImmutableList.of(
                "/target",
                "/target/subdir1",
                "/target/subdir1/subdir1File1",
                "/target/subdir1/subdir1File2",
                "/target/subdir2",
                "/target/subdir2/subdir2File1",
                "/target/subdir2/subdir2File2"
        )));
    }

    @Test
    public void unzipSingleFile() throws Exception {
        Path zip = Paths.get(ZipHandlerTest.class.getResource("/singleFile.zip").toURI());
        Path target = this.fs.getPath(targetDir);

        ZipHandler.unzip(zip, target);

        List<String> result = Files.walk(target).map(Path::toString).sorted().collect(Collectors.toList());
        assertThat(result, is(ImmutableList.of(
                "/target",
                "/target/file1"
        )));
    }

    @Test
    public void unzipSingleSubdir() throws Exception {
        Path zip = Paths.get(ZipHandlerTest.class.getResource("/singleSubdir.zip").toURI());
        Path target = this.fs.getPath(targetDir);

        ZipHandler.unzip(zip, target);

        List<String> result = Files.walk(target).map(Path::toString).sorted().collect(Collectors.toList());
        assertThat(result, is(ImmutableList.of(
                "/target",
                "/target/subdir1File1",
                "/target/subdir1File2"
        )));
    }

    @Test
    public void unzipFilesAndSubdirs() throws Exception {
        Path zip = Paths.get(ZipHandlerTest.class.getResource("/filesAndSubdirs.zip").toURI());
        Path target = this.fs.getPath(targetDir);

        ZipHandler.unzip(zip, target);

        List<String> result = Files.walk(target).map(Path::toString).sorted().collect(Collectors.toList());
        assertThat(result, is(ImmutableList.of(
                "/target",
                "/target/file1",
                "/target/file2",
                "/target/subdir1",
                "/target/subdir1/subdir1File1",
                "/target/subdir1/subdir1File2",
                "/target/subdir2",
                "/target/subdir2/subdir2File1",
                "/target/subdir2/subdir2File2"
        )));
    }
}
