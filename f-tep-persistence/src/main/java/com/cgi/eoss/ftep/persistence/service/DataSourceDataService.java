package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.DataSource;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface DataSourceDataService extends
        FtepEntityDataService<DataSource>,
        SearchableDataService<DataSource> {
    DataSource getByName(String name);

    List<DataSource> findByOwner(User user);
}
