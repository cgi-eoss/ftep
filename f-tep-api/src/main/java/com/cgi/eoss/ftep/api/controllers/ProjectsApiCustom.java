package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectsApiCustom extends BaseRepositoryApi<Project> {
    Page<Project> searchByFilterOnly(String filter, Pageable pageable);

    Page<Project> searchByFilterAndOwner(String filter, User user, Pageable pageable);

    Page<Project> searchByFilterAndNotOwner(String filter, User user, Pageable pageable);
}
