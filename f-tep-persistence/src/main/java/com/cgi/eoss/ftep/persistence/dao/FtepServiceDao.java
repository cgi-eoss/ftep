package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepService;

import java.util.List;

public interface FtepServiceDao extends FtepEntityDao<FtepService> {
    List<FtepService> findByNameContainingIgnoreCase(String term);
}
