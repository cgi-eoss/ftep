package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.User;

import com.google.common.collect.Multimap;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface JobDataService extends FtepEntityDataService<Job> {

    Job getByExtId(UUID extId);

    List<Job> findByOwner(User user);
    List<Job> findByService(FtepService service);
    List<Job> findByOwnerAndService(User user, FtepService service);
    List<Job> findByStartIn(YearMonth yearMonth);

    Job buildNew(String extId, String userId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs);
    Job buildNew(String extId, String userId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs, Job parentJob);

    Job updateJobConfig(Job job);

    Job reload(Long id); // It is used at JobStatus updates
}
