package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface JobConfigDao extends FtepEntityDao<JobConfig> {
    List<JobConfig> findByOwner(User user);

    List<JobConfig> findByService(FtepService service);

    List<JobConfig> findByOwnerAndService(User user, FtepService service);

    List<JobConfig> findByInputFiles(FtepFile file);
}
