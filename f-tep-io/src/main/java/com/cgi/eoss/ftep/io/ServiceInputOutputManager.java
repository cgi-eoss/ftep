package com.cgi.eoss.ftep.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 * <p>Service providing input and output file handling for F-TEP jobs.</p> <p>Generally, this service should provision
 * input files to a given subdirectory (downloading or using a file-object store) and consume job outputs.</p>
 */
public interface ServiceInputOutputManager {

    /**
     * <p>Provision the contents of the given directory from the given uri.</p> <p>Implementations of this may achieve
     * it in different ways, e.g. copy files into the target path, create symlinks, create bind mounts.</p>
     *
     * @param target The directory to be populated. Will be created by this method if it does not exist, but will not be
     * overwritten if it does.
     * @param uris The URIs to resolve and provision in the target directory.
     */
    void prepareInput(Path target, Collection<URI> uris) throws IOException;

    /**
     * <p>Return the path to a directory containing all files necessary to build the Docker image for the given service
     * name.</p>
     */
    Path getServiceContext(String serviceName);

    /**
     * <p>Return true if the given URI scheme (i.e. protocol) is supported by this I/O manager.</p>
     */
    boolean isSupportedProtocol(String scheme);

    /**
     * <p>Process the given URIs to clean up cache entries if necessary.</p>
     *
     * @param unusedUris URIs now unused by any jobs.
     */
    void cleanUp(Set<URI> unusedUris);

}
