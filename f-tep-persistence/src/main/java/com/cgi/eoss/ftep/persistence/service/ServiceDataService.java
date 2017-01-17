package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepUser;

import java.util.List;

public interface ServiceDataService extends
        FtepEntityDataService<FtepService>,
        SearchableDataService<FtepService> {
    List<FtepService> findByOwner(FtepUser user);

    FtepService getByName(String serviceName);
}
