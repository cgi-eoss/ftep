package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.QGroup;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.GroupDao;
import com.google.common.base.Strings;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class GroupsApiImpl extends BaseRepositoryApiImpl<Group> implements GroupsApiCustom {

    private final FtepSecurityService securityService;
    private final GroupDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QGroup.group.id;
    }

    @Override
    QUser getOwnerPath() {
        return QGroup.group.owner;
    }

    @Override
    Class<Group> getEntityClass() {
        return Group.class;
    }

    @Override
    public Page<Group> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(@Param("filter") String filter,
            Pageable pageable) {
        return getFilteredResults(
                QGroup.group.name.containsIgnoreCase(filter).or(QGroup.group.description.containsIgnoreCase(filter)),
                pageable);
    }

    @Override
    public Page<Group> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndOwner(
            @Param("filter") String filter, @Param("owner") User user, Pageable pageable) {
        if (Strings.isNullOrEmpty(filter)) {
            return findByOwner(user, pageable);
        } else {
            return getFilteredResults(getOwnerPath().eq(user).and(QGroup.group.name.containsIgnoreCase(filter)
                    .or(QGroup.group.description.containsIgnoreCase(filter))), pageable);
        }
    }

    @Override
    public Page<Group> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndNotOwner(
            @Param("filter") String filter, @Param("owner") User user, Pageable pageable) {
        if (Strings.isNullOrEmpty(filter)) {
            return findByNotOwner(user, pageable);
        } else {
            return getFilteredResults(getOwnerPath().ne(user).and(QGroup.group.name.containsIgnoreCase(filter)
                    .or(QGroup.group.description.containsIgnoreCase(filter))), pageable);
        }
    }

}
