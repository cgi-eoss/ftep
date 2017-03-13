package com.cgi.eoss.ftep.worker.io;

import com.cgi.eoss.ftep.rpc.GetServiceContextFilesParams;
import com.cgi.eoss.ftep.rpc.ServiceContextFiles;
import com.cgi.eoss.ftep.rpc.ServiceContextFilesServiceGrpc;
import com.cgi.eoss.ftep.rpc.ShortFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class FtepDownloader implements Downloader {

    private final ServiceContextFilesServiceGrpc.ServiceContextFilesServiceBlockingStub serviceContextFilesService;

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        switch(uri.getHost()) {
            case "serviceContext":
                return downloadServiceContextFiles(targetDir, Paths.get(uri.getPath()).getFileName().toString());
            default:
                throw new UnsupportedOperationException("Unrecognised ftep:// URI type: " + uri.getHost());
        }
    }

    private Path downloadServiceContextFiles(Path targetDir, String serviceName) throws IOException {
        ServiceContextFiles serviceContextFiles = serviceContextFilesService.getServiceContextFiles(GetServiceContextFilesParams.newBuilder().setServiceName(serviceName).build());

        for (ShortFile f : serviceContextFiles.getFilesList()) {
            Path targetFile = targetDir.resolve(f.getFilename());
            LOG.debug("Writing service context file for {} to: {}", serviceName, targetFile);
            Files.write(targetFile, f.getContent().toByteArray(), CREATE, TRUNCATE_EXISTING);
        }

        return targetDir;
    }

}
