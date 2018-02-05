package com.cgi.eoss.ftep.zoomanager.service;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * <p>Utility to read and write {@link FtepServiceDescriptor} objects.</p>
 */
public interface FtepServiceDescriptorHandler {

    /**
     * <p>Deserialise the given F-TEP service descriptor from the given file, according to the interface
     * implementation.</p>
     *
     * @param file The file to be read.
     * @return The F-TEP service as described in the given file.
     */
    FtepServiceDescriptor readFile(Path file);

    /**
     * <p>Deserialise the given F-TEP service descriptor from the given stream, according to the interface
     * implementation.</p>
     *
     * @param stream The byte stream representing a service descriptor.
     * @return The F-TEP service as described in the given stream.
     */
    FtepServiceDescriptor read(InputStream stream);

    /**
     * <p>Serialise the given F-TEP service descriptor to the given file, according to the interface implementation.</p>
     *
     * @param svc The F-TEP service descriptor to be serialised.
     * @param file The target file to write.
     */
    void writeFile(FtepServiceDescriptor svc, Path file);

    /**
     * <p>Serialise the given F-TEP service descriptor to the given stream, according to the interface
     * implementation.</p>
     *
     * @param svc The F-TEP service descriptor to be serialised.
     * @param stream The destination for the byte stream.
     */
    void write(FtepServiceDescriptor svc, OutputStream stream);

}
