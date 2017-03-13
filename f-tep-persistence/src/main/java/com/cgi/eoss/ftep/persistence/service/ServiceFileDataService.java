package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;

import java.util.List;

public interface ServiceFileDataService extends
        FtepEntityDataService<FtepServiceContextFile> {
    List<FtepServiceContextFile> findByService(FtepService service);

    List<FtepServiceContextFile> findByService(String serviceName);
}
