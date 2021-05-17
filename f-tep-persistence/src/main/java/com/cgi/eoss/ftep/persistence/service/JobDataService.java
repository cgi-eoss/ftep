package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepFile;
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
    List<Job> findByOwnerAndStartIn(User user, YearMonth yearMonth);

    Job buildNew(String extId, String userId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs, String systematicParameter, List<String> parallelParameters, List<String> searchParameters);
    Job buildNew(String extId, String userId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs, Job parentJob, String systematicParameter, List<String> parallelParameters, List<String> searchParameters);

    Job updateJobConfig(Job job);

    Job reload(Long id); // It is used at JobStatus updates

    List<Job> findByOutputFiles(FtepFile file);

    List<Job> findByStatusAndGuiUrlNotNull(Job.Status status);
}
