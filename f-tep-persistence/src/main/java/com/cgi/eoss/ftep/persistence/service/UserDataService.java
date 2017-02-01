package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.User;

public interface UserDataService extends
        FtepEntityDataService<User>,
        SearchableDataService<User> {
    User getByName(String name);
}
