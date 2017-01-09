package com.cgi.eoss.ftep.orchestrator.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * <p>General contract for a function which downloads (or otherwise retrieves) a URI into a target.</p>
 * <p>Implementations of this may provide functionality for different protocols or environments.</p>
 */
@FunctionalInterface
public interface Downloader {
    /**
     * <p>Download the given URI and output the result in the given target directory.</p>
     *
     * @param targetDir The directory in which the content of the resource at the URI should be written.
     * @param uri The URI to be retrieved.
     * @return The path to the downloaded file.
     */
    Path download(Path targetDir, URI uri) throws IOException;
}
