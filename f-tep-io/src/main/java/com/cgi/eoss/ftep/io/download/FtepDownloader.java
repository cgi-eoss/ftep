package com.cgi.eoss.ftep.io.download;

import com.cgi.eoss.ftep.rpc.FtepServerClient;
import com.cgi.eoss.ftep.rpc.GetServiceContextFilesParams;
import com.cgi.eoss.ftep.rpc.ServiceContextFiles;
import com.cgi.eoss.ftep.rpc.ShortFile;
import com.cgi.eoss.ftep.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.ftep.rpc.catalogue.Databasket;
import com.cgi.eoss.ftep.rpc.catalogue.DatabasketContents;
import com.cgi.eoss.ftep.rpc.catalogue.FileResponse;
import com.cgi.eoss.ftep.rpc.catalogue.FtepFile;
import com.cgi.eoss.ftep.rpc.catalogue.FtepFileUri;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import org.jooq.lambda.Unchecked;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

@Component
@Log4j2
public class FtepDownloader implements Downloader {

    private static final EnumSet<PosixFilePermission> EXECUTABLE_PERMS = EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, GROUP_EXECUTE, OTHERS_READ, OTHERS_EXECUTE);
    private static final EnumSet<PosixFilePermission> NON_EXECUTABLE_PERMS = EnumSet.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ);

    private final FtepServerClient ftepServerClient;
    private final DownloaderFacade downloaderFacade;

    public FtepDownloader(FtepServerClient ftepServerClient, DownloaderFacade downloaderFacade) {
        this.ftepServerClient = ftepServerClient;
        this.downloaderFacade = downloaderFacade;
    }

    @PostConstruct
    public void postConstruct() {
        downloaderFacade.registerDownloader(this);
    }

    @PreDestroy
    public void preDestroy() {
        downloaderFacade.unregisterDownloader(this);
    }

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("ftep");
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        switch (uri.getHost()) {
            case "serviceContext":
                return downloadServiceContextFiles(targetDir, Paths.get(uri.getPath()).getFileName().toString());
            case "outputProduct":
                return downloadFtepFile(targetDir, uri);
            case "refData":
                return downloadFtepFile(targetDir, uri);
            case "databasket":
                return downloadDatabasket(targetDir, uri);
            default:
                throw new UnsupportedOperationException("Unrecognised ftep:// URI type: " + uri.getHost());
        }
    }

    private Path downloadServiceContextFiles(Path targetDir, String serviceName) throws IOException {
        ServiceContextFiles serviceContextFiles = ftepServerClient.serviceContextFilesServiceBlockingStub()
                .getServiceContextFiles(GetServiceContextFilesParams.newBuilder().setServiceName(serviceName).build());

        for (ShortFile f : serviceContextFiles.getFilesList()) {
            Path targetFile = targetDir.resolve(f.getFilename());
            Set<PosixFilePermission> permissions = f.getExecutable() ? EXECUTABLE_PERMS : NON_EXECUTABLE_PERMS;

            LOG.debug("Writing service context file for {} to: {}", serviceName, targetFile);
            Files.write(targetFile, f.getContent().toByteArray(), CREATE, TRUNCATE_EXISTING);
            Files.setPosixFilePermissions(targetFile, permissions);
        }

        return targetDir;
    }

    private Path downloadFtepFile(Path targetDir, URI uri) throws IOException {
        CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueService = ftepServerClient.catalogueServiceBlockingStub();

        Iterator<FileResponse> outputFile = catalogueService.downloadFtepFile(FtepFileUri.newBuilder().setUri(uri.toString()).build());

        // First message is the file metadata
        FileResponse.FileMeta fileMeta = outputFile.next().getMeta();

        // TODO Configure whether files need to be transferred via RPC or simply copied
        Path outputPath = targetDir.resolve(fileMeta.getFilename());
        LOG.info("Transferring FtepFile ({} bytes) to {}", fileMeta.getSize(), outputPath);

        try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outputPath, CREATE, TRUNCATE_EXISTING, WRITE))) {
            outputFile.forEachRemaining(Unchecked.consumer(of -> of.getChunk().getData().writeTo(outputStream)));
        }

        return outputPath;
    }

    private Path downloadDatabasket(Path targetDir, URI uri) {
        CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueService = ftepServerClient.catalogueServiceBlockingStub();

        DatabasketContents databasketContents = catalogueService.getDatabasketContents(Databasket.newBuilder().setUri(uri.toASCIIString()).build());

        databasketContents.getFilesList().forEach(Unchecked.consumer((FtepFile ftepFile) -> {
            URI fileUri = URI.create(ftepFile.getUri().getUri());
            Path cacheDir = downloaderFacade.download(fileUri, null);
            LOG.info("Successfully downloaded from databasket: {} ({})", cacheDir, fileUri);
            symlinkDatabasketContents(cacheDir, targetDir, ftepFile.getFilename());
        }));

        return targetDir;
    }

    private void symlinkDatabasketContents(Path cacheDir, Path targetDir, String filename) throws IOException {
        try (Stream<Path> cacheDirList = Files.list(cacheDir)) {
            Set<Path> cacheDirContents = cacheDirList
                    .filter(f -> Files.isRegularFile(f) && !f.getFileName().toString().equals(".uri"))
                    .collect(Collectors.toSet());

            if (cacheDirContents.size() == 1) {
                // If the cache directory contains only one file (excluding .uri) then symlink it directly
                Path file = Iterables.getOnlyElement(cacheDirContents);
                Files.createSymbolicLink(targetDir.resolve(file.getFileName()), file);
            } else {
                // Otherwise symlink the FtepFile filename to the cache directory
                Files.createSymbolicLink(targetDir.resolve(filename), cacheDir);
            }
        }
    }

}
