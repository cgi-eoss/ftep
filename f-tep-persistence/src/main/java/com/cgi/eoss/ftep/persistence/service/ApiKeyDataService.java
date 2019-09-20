package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.ApiKey;
import com.cgi.eoss.ftep.model.User;

public interface ApiKeyDataService extends FtepEntityDataService<ApiKey> {

    ApiKey getByOwner(User user);
}

