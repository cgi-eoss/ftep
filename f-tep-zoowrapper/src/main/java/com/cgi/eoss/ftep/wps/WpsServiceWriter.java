package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;

import java.nio.file.Path;

/**
 * <p>Utility to generate ZOO-Project WPS service implementations from {@link FtepServiceDescriptor} objects.</p>
 */
public interface WpsServiceWriter {

    /**
     * <p>Create a WPS service implementation from the given service descriptor.</p>
     *
     * @param svc The data to be used to generate the service implementation.
     * @param wpsService The path to be generated. This will overwrite any existing file (or directory if appropriate)
     * in the location.
     */
    void generateWpsService(FtepServiceDescriptor svc, Path wpsService);

}
