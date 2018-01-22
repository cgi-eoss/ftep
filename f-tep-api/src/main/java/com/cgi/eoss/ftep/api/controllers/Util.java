package com.cgi.eoss.ftep.api.controllers;

import com.google.common.io.ByteStreams;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
@UtilityClass
final class Util {

    static void serveFileDownload(HttpServletResponse response, Resource resource) throws IOException {
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLengthLong(resource.contentLength());
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"");
        ByteStreams.copy(resource.getInputStream(), response.getOutputStream());
        response.setStatus(HttpStatus.OK.value());
        response.flushBuffer();
    }

    @Log4j2
    static class ZippingVisitor extends SimpleFileVisitor<Path> {
        private final Path zipBase;
        private final ZipOutputStream zipOut;

        ZippingVisitor(Path zipBase, ZipOutputStream zipOut) {
            super();
            this.zipBase = zipBase;
            this.zipOut = zipOut;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Objects.requireNonNull(file);
            Objects.requireNonNull(attrs);

            LOG.debug("Writing file to zip: {}", file);
            ZipEntry zipEntry = new ZipEntry(zipBase.relativize(file).toString());
            zipOut.putNextEntry(zipEntry);
            try (InputStream fis = Files.newInputStream(file)) {
                ByteStreams.copy(fis, zipOut);
            }

            return FileVisitResult.CONTINUE;
        }
    }
}
