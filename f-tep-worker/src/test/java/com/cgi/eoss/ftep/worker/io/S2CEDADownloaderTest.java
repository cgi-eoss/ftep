package com.cgi.eoss.ftep.worker.io;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.verify;

public class S2CEDADownloaderTest {

    @Mock
    private FtpDownloader ftpDownloader;

    private Path targetPath;

    private FileSystem fs;

    private S2CEDADownloader dl;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.fs = Jimfs.newFileSystem(Configuration.unix());
        this.targetPath = this.fs.getPath("/target");
        Files.createDirectories(targetPath);

        this.dl = new S2CEDADownloader(ftpDownloader);
    }

    @Test
    public void download() throws Exception {
        URI oldStyleProduct = URI.create("sentinel2:///S2A_OPER_PRD_MSIL1C_PDMC_20161030T223944_R083_V20161030T164852_20161030T164852");
        URI newStyleProduct = URI.create("sentinel2:///S2A_MSIL1C_20170428T164901_N0205_R083_T15QVU_20170428T164855");

        dl.download(targetPath, oldStyleProduct);

        verify(ftpDownloader).download(targetPath, URI.create("ftp://ftp.ceda.ac.uk/neodc/sentinel2a/data/L1C_MSI/2016/10/30/S2A_OPER_PRD_MSIL1C_PDMC_20161030T223944_R083_V20161030T164852_20161030T164852.zip"));

        dl.download(targetPath, newStyleProduct);

        verify(ftpDownloader).download(targetPath, URI.create("ftp://ftp.ceda.ac.uk/neodc/sentinel2a/data/L1C_MSI/2017/04/28/S2A_MSIL1C_20170428T164901_N0205_R083_T15QVU_20170428T164855.zip"));
    }

}