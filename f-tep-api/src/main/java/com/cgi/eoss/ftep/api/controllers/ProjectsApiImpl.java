package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.QProject;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.persistence.dao.ProjectDao;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ProjectsApiImpl extends BaseRepositoryApiImpl<Project> {

    private final FtepSecurityService securityService;
    private final ProjectDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QProject.project.id;
    }

    @Override
    QUser getOwnerPath() {
        return QProject.project.owner;
    }

    @Override
    Class<Project> getEntityClass() {
        return Project.class;
    }

}
