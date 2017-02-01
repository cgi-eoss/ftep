package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.User;
import com.google.common.collect.Multimap;

import java.util.List;

public interface JobDataService extends
        FtepEntityDataService<Job> {

    List<Job> findByOwner(User user);

    List<Job> findByService(FtepService service);

    List<Job> findByOwnerAndService(User user, FtepService service);

    Job buildNew(String extId, String userId, String serviceId, Multimap<String, String> inputs);
}
