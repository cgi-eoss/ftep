package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.ApiKey;
import com.cgi.eoss.ftep.model.User;

public interface ApiKeyDao extends FtepEntityDao<ApiKey> {

    public ApiKey getByOwner(User owner);
}
