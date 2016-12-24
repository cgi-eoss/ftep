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
     * <p>Download the given URI and output the result to the given target.</p>
     *
     * @param target The destination to which the content of the resource at the URI should be written.
     * @param uri The URI to be retrieved.
     */
    void download(Path target, URI uri) throws IOException;
}
