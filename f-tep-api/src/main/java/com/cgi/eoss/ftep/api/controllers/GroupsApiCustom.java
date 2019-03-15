package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface GroupsApiCustom extends BaseRepositoryApi<Group> {
    <S extends Group> S save(S group);

    Page<Group> findByFilterOnly(String filter, Pageable pageable);

    Page<Group> findByFilterAndOwner(String filter, User user, Pageable pageable);

    Page<Group> findByFilterAndNotOwner(String filter, User user, Pageable pageable);
}
