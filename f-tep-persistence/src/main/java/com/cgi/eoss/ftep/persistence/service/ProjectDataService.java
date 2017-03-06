package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface ProjectDataService extends
        FtepEntityDataService<Project>,
        SearchableDataService<Project> {
    Project getByNameAndOwner(String name, User user);

    List<Project> findByDatabasket(Databasket databasket);

    List<Project> findByJobConfig(JobConfig jobConfig);

    List<Project> findByOwner(User user);
}
