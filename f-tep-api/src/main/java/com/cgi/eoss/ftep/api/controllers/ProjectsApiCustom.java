package com.cgi.eoss.ftep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.User;

public interface ProjectsApiCustom {
    Page<Project> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(@Param("filter") String filter,
            Pageable pageable);

    Page<Project> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndOwner(@Param("filter") String filter,
            @Param("owner") User user, Pageable pageable);

    Page<Project> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndNotOwner(@Param("filter") String filter,
            @Param("owner") User user, Pageable pageable);
}
