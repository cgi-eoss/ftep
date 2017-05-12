package com.cgi.eoss.ftep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.User;

public interface DatabasketsApiCustom {

    Page<Databasket> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(@Param("filter") String filter,
            Pageable pageable);

    Page<Databasket> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndOwner(@Param("filter") String filter,
            @Param("owner") User user, Pageable pageable);

    Page<Databasket> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndNotOwner(
            @Param("filter") String filter, @Param("owner") User user, Pageable pageable);
}
