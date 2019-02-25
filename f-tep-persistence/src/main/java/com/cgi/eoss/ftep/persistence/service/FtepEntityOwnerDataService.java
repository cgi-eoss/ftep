package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepEntityWithOwner;
import com.cgi.eoss.ftep.model.User;

public interface FtepEntityOwnerDataService {
    User getOwner(Class<? extends FtepEntityWithOwner> entityClass, Long id);
}
