package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.DataSource;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface DataSourceDataService extends
        FtepEntityDataService<DataSource>,
        SearchableDataService<DataSource> {
    DataSource getByName(String name);

    List<DataSource> findByOwner(User user);

    DataSource getForService(FtepService service);

    DataSource getForExternalProduct(FtepFile ftepFile);

    DataSource getForRefData(FtepFile ftepFile);
}
