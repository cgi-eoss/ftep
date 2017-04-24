package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.DataSource;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface DataSourceDao extends FtepEntityDao<DataSource> {
    List<DataSource> findByNameContainingIgnoreCase(String term);
    DataSource findOneByName(String name);
    List<DataSource> findByOwner(User user);
}
