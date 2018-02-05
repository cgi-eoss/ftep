package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.User;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface UserDao extends FtepEntityDao<User> {
    @EntityGraph(attributePaths = {"groups"})
    User findOneByName(String name);
    List<User> findByNameContainingIgnoreCase(String term);
}
