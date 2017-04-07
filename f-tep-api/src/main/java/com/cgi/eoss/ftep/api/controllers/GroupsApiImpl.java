package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.QGroup;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.persistence.dao.GroupDao;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class GroupsApiImpl extends BaseRepositoryApiImpl<Group> {

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

}
