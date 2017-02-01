package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface GroupDao extends FtepEntityDao<Group> {
    List<Group> findByNameContainingIgnoreCase(String term);

    List<Group> findByMembersContaining(User member);

    List<Group> findByOwner(User user);
}
