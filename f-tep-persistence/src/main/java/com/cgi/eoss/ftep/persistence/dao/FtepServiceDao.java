package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface FtepServiceDao extends FtepEntityDao<FtepService> {
    List<FtepService> findByNameContainingIgnoreCase(String term);

    List<FtepService> findByOwner(User user);
}
