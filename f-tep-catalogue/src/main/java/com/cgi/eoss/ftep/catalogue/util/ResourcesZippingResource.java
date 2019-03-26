package com.cgi.eoss.ftep.catalogue.util;

import com.google.common.io.ByteStreams;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
public class ResourcesZippingResource extends AbstractResource implements Resource {
    private static final int DEFAULT_STREAM_BUFFER_SIZE = 8192 * 4;

    private final String filename;
    private final Map<String, Resource> resources;
    private CompletableFuture<Void> zippingFuture;

    public ResourcesZippingResource(String filename, Map<String, Resource> resources) {
        this.filename = filename;
        this.resources = resources;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long contentLength() {
        return -1L;
    }

    @Override
    public String getDescription() {
        return "ResourcesZippingResource[" + resources.size() + "]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);

        // Use a thread to asynchronously write the zipped directory specified
        // to the given PipedOutputStream. This thread ensures that the reading
        // from the associated PipedInputStream is not blocked indefinitely
        // while the zip is written to the output buffer but nothing is
        // consuming from the associated pipe. Such behaviour would be fine if
        // the total output size is smaller than the buffer, but this is not
        // guaranteed.
        zippingFuture = CompletableFuture.runAsync(() -> {
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(pos, DEFAULT_STREAM_BUFFER_SIZE))) {
                zos.setLevel(Deflater.BEST_SPEED);
                for (Map.Entry<String, Resource> entry : resources.entrySet()) {
                    String path = entry.getKey();
                    Resource resource = entry.getValue();
                    LOG.debug("Writing resource to zip at path {}: {}", path, resource);
                    zos.putNextEntry(new ZipEntry(path));
                    ByteStreams.copy(resource.getInputStream(), zos);
                    zos.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return pis;
    }

    public CompletableFuture<Void> getZippingFuture() {
        return zippingFuture;
    }

}
