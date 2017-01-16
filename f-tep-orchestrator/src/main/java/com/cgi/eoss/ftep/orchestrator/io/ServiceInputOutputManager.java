package com.cgi.eoss.ftep.orchestrator.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * <p>Service providing input and output file handling for F-TEP jobs.</p>
 * <p>Generally, this service should provision input files to a given subdirectory (downloading or using a file-object
 * store) and consume job outputs.</p>
 */
public interface ServiceInputOutputManager {

    /**
     * <p>Provision the contents of the given directory from the given uri.</p>
     * <p>Implementations of this may achieve it in different ways, e.g. copy files into the target path, create
     * symlinks, create bind mounts.</p>
     *
     * @param target The directory to be populated. Will be created by this method if it does not exist, but will not be
     * overwritten if it does.
     * @param uri The URI to resolve and provision in the target directory.
     */
    void prepareInput(Path target, URI uri) throws IOException;

}
