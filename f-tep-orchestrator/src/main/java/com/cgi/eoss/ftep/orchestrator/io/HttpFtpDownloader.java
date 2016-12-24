package com.cgi.eoss.ftep.orchestrator.io;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

public class HttpFtpDownloader implements Downloader {
    @Override
    public void download(Path target, URI uri) throws IOException {
        URL url = uri.toURL();

        WGet wGet;
        if (uri.getScheme().equals("http")) {
            // DownloadInfo enables resuming downloads
            DownloadInfo downloadInfo = new DownloadInfo(url);
            downloadInfo.extract();
            wGet = new WGet(downloadInfo, target.toFile());
        } else {
            wGet = new WGet(url, target.toFile());
        }

        wGet.download();
    }
}
