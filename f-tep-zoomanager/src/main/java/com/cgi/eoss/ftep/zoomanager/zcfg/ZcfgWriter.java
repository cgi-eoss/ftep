package com.cgi.eoss.ftep.zoomanager.zcfg;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;

import java.nio.file.Path;
import java.util.Set;

/**
 * <p>Utility to generate ZOO-Project zcfg files from {@link FtepServiceDescriptor} objects.</p>
 */
public interface ZcfgWriter {

    /**
     * <p>Serialise the given {@link FtepServiceDescriptor} object in zcfg format, for use by zoo_loader.cgi.</p>
     *
     * @param svc The service data to be serialised.
     * @param zcfg The path to the file to be generated. This will overwrite any existing file in the location.
     */
    void generateZcfg(FtepServiceDescriptor svc, Path zcfg);

    /**
     * <p>Serialise the given {@link FtepServiceDescriptor} objects in zcfg format, for use by zoo_loader.cgi.</p>
     *
     * @param services The service data to be serialised.
     * @param zcfgBasePath The path to a directory to contian the zcfg files. This will remove any existing .zcfg files
     * in the location.
     */
    void generateZcfgs(Set<FtepServiceDescriptor> services, Path zcfgBasePath);

}
