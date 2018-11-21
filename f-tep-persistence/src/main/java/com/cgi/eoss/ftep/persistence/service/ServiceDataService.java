package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface ServiceDataService extends FtepEntityDataService<FtepService>, SearchableDataService<FtepService> {
    List<FtepService> findAllAvailable();
    List<FtepService> findByOwner(User user);

    FtepService getByName(String serviceName);
}
