package com.cgi.eoss.ftep.catalogue;

import com.cgi.eoss.ftep.model.FtepFile;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 */
public interface FtepFileService {
    Resource resolve(FtepFile file);
    void delete(FtepFile file) throws IOException;
}
