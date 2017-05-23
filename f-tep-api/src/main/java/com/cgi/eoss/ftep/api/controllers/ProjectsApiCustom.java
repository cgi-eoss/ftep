package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectsApiCustom {
    Page<Project> findByFilterOnly(String filter, Pageable pageable);

    Page<Project> findByFilterAndOwner(String filter, User user, Pageable pageable);

    Page<Project> findByFilterAndNotOwner(String filter, User user, Pageable pageable);
}
