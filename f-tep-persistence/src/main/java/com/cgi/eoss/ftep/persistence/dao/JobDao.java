package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface JobDao extends FtepEntityDao<Job> {
    List<Job> findByOwner(User user);

    List<Job> findByConfig_Service(FtepService service);

    List<Job> findByOwnerAndConfig_Service(User user, FtepService service);

    List<Job> findByOutputFiles(FtepFile file);
}
