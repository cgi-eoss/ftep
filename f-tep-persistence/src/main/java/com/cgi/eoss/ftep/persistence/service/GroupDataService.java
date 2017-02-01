package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface GroupDataService extends
        FtepEntityDataService<Group>,
        SearchableDataService<Group> {

    List<Group> findGroupMemberships(User user);

    List<Group> findByOwner(User user);
}
