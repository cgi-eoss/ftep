package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepProject;
import com.cgi.eoss.ftep.model.FtepUser;

import java.util.List;

public interface FtepProjectDao extends FtepEntityDao<FtepProject> {
    List<FtepProject> findByNameContainingIgnoreCase(String term);

    List<FtepProject> findByOwner(FtepUser user);
}
