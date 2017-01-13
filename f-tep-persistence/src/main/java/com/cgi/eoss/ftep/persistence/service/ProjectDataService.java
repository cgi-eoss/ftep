package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepProject;
import com.cgi.eoss.ftep.model.FtepUser;

import java.util.List;

public interface ProjectDataService extends
        FtepEntityDataService<FtepProject>,
        SearchableDataService<FtepProject> {
    List<FtepProject> findByOwner(FtepUser user);
}
