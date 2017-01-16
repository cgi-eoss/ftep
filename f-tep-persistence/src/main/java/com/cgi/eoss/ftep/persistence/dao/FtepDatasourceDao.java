package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepDatasource;

import java.util.List;

public interface FtepDatasourceDao extends FtepEntityDao<FtepDatasource> {
    List<FtepDatasource> findByNameContainingIgnoreCase(String term);
}
