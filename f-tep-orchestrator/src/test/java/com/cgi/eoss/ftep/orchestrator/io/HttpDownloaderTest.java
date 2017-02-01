package com.cgi.eoss.ftep.orchestrator.io;

import com.cgi.eoss.ftep.model.DownloaderCredentials;
import com.cgi.eoss.ftep.persistence.service.DownloaderCredentialsDataService;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class HttpDownloaderTest {
    @Mock
    private DownloaderCredentialsDataService credentialsDataService;

    private Path targetPath;

    private FileSystem fs;

    private Downloader dl;

    private MockWebServer webServer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.fs = Jimfs.newFileSystem(Configuration.unix());
        this.targetPath = this.fs.getPath("/target");
        Files.createDirectories(targetPath);

        this.webServer = buildFakeWebServer();
        this.webServer.start();

        this.dl = new HttpDownloader(credentialsDataService);
    }

    @Test
    public void testAnonymous() throws Exception {
        URI uri = webServer.url("/data/testfile.txt").uri();

        InputStream testFileIs = Files.newInputStream(Paths.get(HttpDownloaderTest.class.getResource("/testfile.txt").toURI()));
        webServer.enqueue(new MockResponse().setBody(new Buffer().readFrom(testFileIs)));

        Path download = dl.download(targetPath, uri);

        assertThat(download, is(targetPath.resolve("testfile.txt")));
        assertThat(Files.readAllLines(download), is(ImmutableList.of("foo bar baz")));
    }

    @Test
    public void testAuthenticated() throws Exception {
        URI uri = webServer.url("/data/testfile.txt").uri();

        InputStream testFileIs = Files.newInputStream(Paths.get(HttpDownloaderTest.class.getResource("/testfile.txt").toURI()));
        webServer.enqueue(new MockResponse().setResponseCode(401));
        webServer.enqueue(new MockResponse().setBody(new Buffer().readFrom(testFileIs)));

        when(credentialsDataService.getByHost(any())).thenReturn(DownloaderCredentials.basicBuilder()
                .username("httpuser")
                .password("httppass")
                .build());

        Path download = dl.download(targetPath, uri);

        RecordedRequest request1 = webServer.takeRequest();
        RecordedRequest request2 = webServer.takeRequest();

        assertThat(request1.getHeader("Authorization"), is(nullValue()));
        assertThat(request2.getHeader("Authorization"), is(notNullValue()));

        assertThat(download, is(targetPath.resolve("testfile.txt")));
        assertThat(Files.readAllLines(download), is(ImmutableList.of("foo bar baz")));
    }

    @Test
    public void testRenameFile() throws Exception {
        URI uri = webServer.url("/data/testfile.txt").uri();

        InputStream testFileIs = Files.newInputStream(Paths.get(HttpDownloaderTest.class.getResource("/testfile.txt").toURI()));
        webServer.enqueue(new MockResponse()
                .setHeader("Content-Disposition", "attachment; filename=\"newfilename.txt\"")
                .setBody(new Buffer().readFrom(testFileIs)));

        Path download = dl.download(targetPath, uri);

        assertThat(download, is(targetPath.resolve("newfilename.txt")));
        assertThat(Files.readAllLines(download), is(ImmutableList.of("foo bar baz")));
    }

    private MockWebServer buildFakeWebServer() throws Exception {
        return new MockWebServer();
    }

}