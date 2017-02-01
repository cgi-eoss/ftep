package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface JobConfigDataService extends
        FtepEntityDataService<JobConfig> {

    List<JobConfig> findByOwner(User user);

    List<JobConfig> findByService(FtepService service);

    List<JobConfig> findByOwnerAndService(User user, FtepService service);

}
