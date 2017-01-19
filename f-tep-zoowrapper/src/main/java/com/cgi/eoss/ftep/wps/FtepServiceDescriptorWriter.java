package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;

import java.nio.file.Path;

/**
 * <p>Utility to write an {@link FtepServiceDescriptor} object to a file.</p>
 */
@FunctionalInterface
public interface FtepServiceDescriptorWriter {

    /**
     * <p>Serialise the given F-TEP service descriptor to the given file, according to the interface implementation.</p>
     *
     * @param svc The F-TEP service descriptor to be serialised.
     * @param file The target file to write.
     */
    void writeFile(FtepServiceDescriptor svc, Path file);

}
