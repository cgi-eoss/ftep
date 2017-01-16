package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepJob;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepUser;

import java.util.List;

public interface JobDataService extends
        FtepEntityDataService<FtepJob> {

    List<FtepJob> findByOwner(FtepUser user);

    List<FtepJob> findByService(FtepService service);

    List<FtepJob> findByOwnerAndService(FtepUser user, FtepService service);

}
