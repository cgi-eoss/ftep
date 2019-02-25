package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

interface BaseRepositoryApi<T> {
    Page<T> findAll(Pageable pageable);

    <S extends T> S save(S entity);

    @RestResource(path = "findByOwner", rel = "findByOwner")
    Page<T> findByOwner(@Param("owner") User owner, Pageable pageable);

    @RestResource(path = "findByNotOwner", rel = "findByNotOwner")
    Page<T> findByOwnerNot(@Param("owner") User owner, Pageable pageable);
}
