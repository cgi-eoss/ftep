package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface ProjectDao extends FtepEntityDao<Project> {
    Project findOneByNameAndOwner(String name, User user);

    List<Project> findByNameContainingIgnoreCase(String term);

    List<Project> findByDatabasketsContaining(Databasket databasket);

    List<Project> findByJobConfigsContaining(JobConfig jobConfig);

    List<Project> findByOwner(User user);
}
