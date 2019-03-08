package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface ServiceDataService extends FtepEntityDataService<FtepService>, SearchableDataService<FtepService> {
    public List<FtepService> findAllAvailable();
    public List<FtepService> findByOwner(User user);

    public FtepService getByName(String serviceName);

    public String computeServiceFingerprint(FtepService ftepService);
}
