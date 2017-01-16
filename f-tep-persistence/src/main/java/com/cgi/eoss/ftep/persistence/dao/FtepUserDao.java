package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepUser;

import java.util.List;

public interface FtepUserDao extends FtepEntityDao<FtepUser> {
    List<FtepUser> findByNameContainingIgnoreCase(String term);
}
