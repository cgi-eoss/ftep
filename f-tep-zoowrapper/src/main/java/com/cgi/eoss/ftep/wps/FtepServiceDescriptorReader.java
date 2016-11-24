package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;

import java.nio.file.Path;

public interface FtepServiceDescriptorReader {

    FtepServiceDescriptor readFile(Path file);

}
