package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;

import java.nio.file.Path;

/**
 * <p>Utility to generate ZOO-Project zcfg files from {@link FtepServiceDescriptor} objects.</p>
 */
@FunctionalInterface
public interface ZcfgWriter {

    /**
     * <p>Serialise the given {@link FtepServiceDescriptor} object in zcfg format, for use by zoo_loader.cgi.</p>
     *
     * @param svc The service data to be serialised.
     * @param zcfg The path to the file to be generated. This will overwrite any existing file in the location.
     */
    void generateZcfg(FtepServiceDescriptor svc, Path zcfg);

}
