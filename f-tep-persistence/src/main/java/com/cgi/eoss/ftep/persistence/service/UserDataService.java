package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepUser;

public interface UserDataService extends
        FtepEntityDataService<FtepUser>,
        SearchableDataService<FtepUser> {
}
