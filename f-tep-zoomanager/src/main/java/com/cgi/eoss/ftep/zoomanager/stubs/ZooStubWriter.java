package com.cgi.eoss.ftep.zoomanager.stubs;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;

import java.nio.file.Path;
import java.util.Set;

/**
 * <p>Utility to generate ZOO-Project WPS service implementations from {@link FtepServiceDescriptor} objects.</p>
 */
@FunctionalInterface
public interface ZooStubWriter {

    /**
     * <p>Create a jar containing ZOO-Project stub WPS service implementation classes from the given service
     * descriptors.</p>
     * <p>The generated library should be added to the ZOO-Kernel JNI CLASSPATH, along with any dependencies, to provide
     * WPS service implementations to ZOO.</p>
     *
     * @param services The data to be used to generate the stub library.
     * @param jar The destination jar file path. Any existing file at this location will be overwritten.
     */
    void generateWpsStubLibrary(Set<FtepServiceDescriptor> services, Path jar);

}
