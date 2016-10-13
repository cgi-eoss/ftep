package com.cgi.eoss.ftep.data.manager.core;

import com.cgi.eoss.ftep.core.utils.beans.ParameterId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.nio.file.Files.isSymbolicLink;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class MultiTenantFileDataManagerTest {

    private static URL URL_FOO;
    private static URL URL_BAR;
    private static URL URL_BAZ;
    private static Map<ParameterId, List<URL>> INPUT_PARAMS;

    static {
        try {
            URL_FOO = new URL("http://host1.example.com/foo.txt");
            URL_BAR = new URL("http://host1.example.com/bar.txt");
            URL_BAZ = new URL("ftp://host1.example.com/foo/bar/baz.zip");
            INPUT_PARAMS = ImmutableMap.<ParameterId, List<URL>>builder()
                    .put(ParameterId.of("Input1"), ImmutableList.of(URL_BAZ))
                    .put(ParameterId.of("Input2"), ImmutableList.of(URL_FOO, URL_BAR))
                    .build();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Mock
    private SecpDownloader secpDownloader;

    private Path downloadScript;

    private Path downloadDir;

    private Path destinationDir;

    private FileSystem fs;

    @InjectMocks
    private MultiTenantFileDataManager dm;

    @Before
    public void setUp() throws Exception {
        fs = Jimfs.newFileSystem(Configuration.unix());

        downloadScript = Files.createDirectories(fs.getPath("/opt/secp/bin/secp"));
        downloadDir = Files.createDirectories(fs.getPath("/data/cache"));
        destinationDir = Files.createDirectories(fs.getPath("/path/to/jobDir/in"));
    }

    @Test
    public void getDataEmpty() throws Exception {
        Map<URL, MultiTenantFileDataManager.DownloadTask> results = dm.getData(
                new DownloaderConfiguration(downloadDir, downloadScript), destinationDir, INPUT_PARAMS);

        for (MultiTenantFileDataManager.DownloadTask r : results.values()) {
            assertThat(r.getSymlink(), is(nullValue()));
        }
    }

    @Test
    public void getDataZipFailure() throws Exception {
        String fooHash = Hashing.sha1().hashString(URL_FOO.toString(), Charset.forName("UTF-8")).toString();
        String barHash = Hashing.sha1().hashString(URL_BAR.toString(), Charset.forName("UTF-8")).toString();
        String bazHash = Hashing.sha1().hashString(URL_BAZ.toString(), Charset.forName("UTF-8")).toString();

        Path fooDir = downloadDir.resolve(fooHash);
        Path fooTmpDir = downloadDir.resolve(fooHash + ".part");
        Path barDir = downloadDir.resolve(barHash);
        Path barTmpDir = downloadDir.resolve(barHash + ".part");
        Path bazDir = downloadDir.resolve(bazHash);
        Path bazTmpDir = downloadDir.resolve(bazHash + ".part");

        when(secpDownloader.download(downloadScript, fooTmpDir, URL_FOO)).thenAnswer(new Answer<Object>() {
            @Override
            public Path answer(InvocationOnMock invocation) throws Throwable {
                return Files.createFile(fooTmpDir.resolve("foo.txt"));
            }
        });
        when(secpDownloader.download(downloadScript, barTmpDir, URL_BAR)).thenAnswer(new Answer<Object>() {
            @Override
            public Path answer(InvocationOnMock invocation) throws Throwable {
                return Files.createFile(barTmpDir.resolve("bar.txt"));
            }
        });
        when(secpDownloader.download(downloadScript, bazTmpDir, URL_BAZ)).thenAnswer(new Answer<Object>() {
            @Override
            public Path answer(InvocationOnMock invocation) throws Throwable {
                return Files.createFile(bazTmpDir.resolve("baz.zip"));
            }
        });

        Map<URL, MultiTenantFileDataManager.DownloadTask> results = dm.getData(
                new DownloaderConfiguration(downloadDir, downloadScript), destinationDir, INPUT_PARAMS);

        // With no data the text files should have succeeded, but the zip file should have failed
        assertThat(results.get(URL_FOO).isSuccess(), is(true));
        assertThat(results.get(URL_FOO).getSymlink(), is(destinationDir.resolve("foo.txt")));
        assertThat(isSymbolicLink(results.get(URL_FOO).getSymlink()), is(true));
        assertThat(Files.readSymbolicLink(results.get(URL_FOO).getSymlink()), is(fooDir.resolve("foo.txt")));

        assertThat(results.get(URL_BAR).isSuccess(), is(true));
        assertThat(results.get(URL_BAR).getSymlink(), is(destinationDir.resolve("bar.txt")));
        assertThat(isSymbolicLink(results.get(URL_BAR).getSymlink()), is(true));
        assertThat(Files.readSymbolicLink(results.get(URL_BAR).getSymlink()), is(barDir.resolve("bar.txt")));

        assertThat(results.get(URL_BAZ).isSuccess(), is(false));
        assertThat(results.get(URL_BAZ).getSymlink(), is(nullValue()));
        // The broken zip file should be cleaned up
        assertThat(Files.exists(bazTmpDir), is(false));
    }

    @Test
    public void getData() throws Exception {
        String fooHash = Hashing.sha1().hashString(URL_FOO.toString(), Charset.forName("UTF-8")).toString();
        String barHash = Hashing.sha1().hashString(URL_BAR.toString(), Charset.forName("UTF-8")).toString();
        String bazHash = Hashing.sha1().hashString(URL_BAZ.toString(), Charset.forName("UTF-8")).toString();

        Path fooDir = downloadDir.resolve(fooHash);
        Path fooTmpDir = downloadDir.resolve(fooHash + ".part");
        Path barDir = downloadDir.resolve(barHash);
        Path barTmpDir = downloadDir.resolve(barHash + ".part");
        Path bazDir = downloadDir.resolve(bazHash);
        Path bazTmpDir = downloadDir.resolve(bazHash + ".part");

        when(secpDownloader.download(downloadScript, fooTmpDir, URL_FOO)).thenAnswer(new Answer<Object>() {
            @Override
            public Path answer(InvocationOnMock invocation) throws Throwable {
                return Files.createFile(fooTmpDir.resolve("foo.txt"));
            }
        });
        when(secpDownloader.download(downloadScript, barTmpDir, URL_BAR)).thenAnswer(new Answer<Object>() {
            @Override
            public Path answer(InvocationOnMock invocation) throws Throwable {
                return Files.createFile(barTmpDir.resolve("bar.txt"));
            }
        });
        when(secpDownloader.download(downloadScript, bazTmpDir, URL_BAZ)).thenAnswer(new Answer<Object>() {
            @Override
            public Path answer(InvocationOnMock invocation) throws Throwable {
                Path bazZipFile = bazTmpDir.resolve("baz.zip");
                Files.copy(MultiTenantFileDataManager.class.getResourceAsStream("baz.zip"), bazZipFile, REPLACE_EXISTING);
                return bazZipFile;
            }
        });

        Map<URL, MultiTenantFileDataManager.DownloadTask> results = dm.getData(
                new DownloaderConfiguration(downloadDir, downloadScript), destinationDir, INPUT_PARAMS);

        // With no data the text files should have succeeded, but the zip file should have failed
        assertThat(results.get(URL_FOO).isSuccess(), is(true));
        assertThat(results.get(URL_FOO).getSymlink(), is(destinationDir.resolve("foo.txt")));
        assertThat(isSymbolicLink(results.get(URL_FOO).getSymlink()), is(true));
        assertThat(Files.readSymbolicLink(results.get(URL_FOO).getSymlink()), is(fooDir.resolve("foo.txt")));

        assertThat(results.get(URL_BAR).isSuccess(), is(true));
        assertThat(results.get(URL_BAR).getSymlink(), is(destinationDir.resolve("bar.txt")));
        assertThat(isSymbolicLink(results.get(URL_BAR).getSymlink()), is(true));
        assertThat(Files.readSymbolicLink(results.get(URL_BAR).getSymlink()), is(barDir.resolve("bar.txt")));

        assertThat(results.get(URL_BAZ).isSuccess(), is(true));
        assertThat(results.get(URL_BAZ).getSymlink(), is(destinationDir.resolve("baz")));
        assertThat(Files.readSymbolicLink(results.get(URL_BAZ).getSymlink()), is(bazDir));
        // The zip file should be cleaned up
        assertThat(Files.exists(bazTmpDir), is(false));
    }

}