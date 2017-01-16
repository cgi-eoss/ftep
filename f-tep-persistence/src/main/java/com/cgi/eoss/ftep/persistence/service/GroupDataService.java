package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepGroup;
import com.cgi.eoss.ftep.model.FtepUser;

import java.util.List;

public interface GroupDataService extends
        FtepEntityDataService<FtepGroup>,
        SearchableDataService<FtepGroup> {

    List<FtepGroup> findGroupMemberships(FtepUser user);

    List<FtepGroup> findByOwner(FtepUser user);
}
