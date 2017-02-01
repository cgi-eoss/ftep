package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface UserDao extends FtepEntityDao<User> {
    List<User> findByNameContainingIgnoreCase(String term);
}
