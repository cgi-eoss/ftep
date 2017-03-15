package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.internal.CompleteFtepService;

import java.util.List;

public interface ServiceDataService extends
        FtepEntityDataService<FtepService>,
        SearchableDataService<FtepService> {
    List<FtepService> findByOwner(User user);

    FtepService getByName(String serviceName);

    CompleteFtepService save(CompleteFtepService service);
}
