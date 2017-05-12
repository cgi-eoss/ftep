package com.cgi.eoss.ftep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.User;

public interface ServicesApiCustom {
    Page<FtepService> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(@Param("filter") String filter,
            Pageable pageable);

    Page<FtepService> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndOwner(@Param("filter") String filter,
            @Param("owner") User user, Pageable pageable);

    Page<FtepService> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndNotOwner(@Param("filter") String filter,
            @Param("owner") User user, Pageable pageable);
}
