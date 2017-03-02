package com.cgi.eoss.ftep.catalogue.files;

import com.cgi.eoss.ftep.model.FtepFile;
import org.springframework.core.io.Resource;

/**
 */
public interface FtepFileService {
    Resource resolve(FtepFile file);
}
