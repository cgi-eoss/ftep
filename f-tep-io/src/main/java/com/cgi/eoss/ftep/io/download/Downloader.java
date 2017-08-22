package com.cgi.eoss.ftep.io.download;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;

/**
 * <p>General contract for a function which downloads (or otherwise retrieves) a URI into a target.</p>
 * <p>Implementations of this may provide functionality for different protocols or environments.</p>
 */
public interface Downloader {

    /**
     * @return The URI schemes (i.e. protocols) for which this downloader is valid. May return an empty collection for a
     * "utility" downloader implementation.
     */
    Set<String> getProtocols();

    /**
     * @return The priority this downloader should be take when processing the given URI. A downloader returning a
     * higher value will be used in preference to one returning a lower value. Default priority is 0.
     */
    default int getPriority(URI uri) {
        return 0;
    }

    /**
     * <p>Download the given URI and output the result in the given target directory.</p>
     *
     * @param targetDir The directory in which the content of the resource at the URI should be written.
     * @param uri The URI to be retrieved.
     * @return The path to the downloaded file.
     */
    Path download(Path targetDir, URI uri) throws IOException;
}
