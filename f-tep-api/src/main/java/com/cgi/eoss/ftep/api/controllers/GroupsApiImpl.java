package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.QGroup;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.GroupDao;
import com.google.common.base.Strings;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<Group> findByFilterOnly(String filter, Pageable pageable) {
        return getFilteredResults(
                getFilterExpression(filter),
                pageable);
    }

    @Override
    public Page<Group> findByFilterAndOwner(String filter, User user, Pageable pageable) {
        if (Strings.isNullOrEmpty(filter)) {
            return findByOwner(user, pageable);
        } else {
            return getFilteredResults(getOwnerPath().eq(user).and(getFilterExpression(filter)), pageable);
        }
    }

    @Override
    public Page<Group> findByFilterAndNotOwner(String filter, User user, Pageable pageable) {
        if (Strings.isNullOrEmpty(filter)) {
            return findByNotOwner(user, pageable);
        } else {
            return getFilteredResults(getOwnerPath().ne(user).and(getFilterExpression(filter)), pageable);
        }
    }

    private BooleanExpression getFilterExpression(String filter) {
        return QGroup.group.name.containsIgnoreCase(filter)
                .or(QGroup.group.description.containsIgnoreCase(filter));
    }

}
