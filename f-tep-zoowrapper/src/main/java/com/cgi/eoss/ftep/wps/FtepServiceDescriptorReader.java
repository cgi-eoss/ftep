package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;

import java.nio.file.Path;

/**
 * <p>Utility to read an {@link FtepServiceDescriptor} object from a file.</p>
 */
@FunctionalInterface
public interface FtepServiceDescriptorReader {

    /**
     * <p>Deserialise the given F-TEP service descriptor from the given file, according to the interface
     * implementation.</p>
     *
     * @param file The file to be read.
     * @return The F-TEP service as described in the given file.
     */
    FtepServiceDescriptor readFile(Path file);

}
