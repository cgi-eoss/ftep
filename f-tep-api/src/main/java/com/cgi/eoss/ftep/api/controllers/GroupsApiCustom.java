package com.cgi.eoss.ftep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;

public interface GroupsApiCustom {
    Page<Group> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(@Param("filter") String filter, Pageable pageable);

    Page<Group> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndOwner(@Param("filter") String filter,
            @Param("owner") User user, Pageable pageable);

    Page<Group> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndNotOwner(@Param("filter") String filter,
            @Param("owner") User user, Pageable pageable);
}
