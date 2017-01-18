package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepDatasource;
import com.cgi.eoss.ftep.model.internal.Credentials;

public interface DatasourceDataService extends
        FtepEntityDataService<FtepDatasource>,
        SearchableDataService<FtepDatasource> {

    /**
     * @param host The host/domain name for which credentials are required.
     * @return The F-TEP credentials for interacting with the given host. If no credentials exist for the given domain,
     * an empty Credentials object is returned.
     */
    Credentials getCredentials(String host);
}
