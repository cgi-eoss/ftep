package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.QProject;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.ProjectDao;
import com.google.common.base.Strings;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ProjectsApiImpl extends BaseRepositoryApiImpl<Project> implements ProjectsApiCustom {

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

    @Override
    public Page<Project> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(@Param("filter") String filter,
            Pageable pageable) {
        return getFilteredResults(QProject.project.name.containsIgnoreCase(filter)
                .or(QProject.project.description.containsIgnoreCase(filter)), pageable);
    }

    @Override
    public Page<Project> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndOwner(
            @Param("filter") String filter, @Param("owner") User user, Pageable pageable) {
        if(Strings.isNullOrEmpty(filter)){
            return findByOwner(user, pageable);
        }
        else{
            return getFilteredResults(getOwnerPath().eq(user).and(QProject.project.name.containsIgnoreCase(filter)
                    .or(QProject.project.description.containsIgnoreCase(filter))), pageable);
        }
    }

    @Override
    public Page<Project> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndNotOwner(
            @Param("filter") String filter, @Param("owner") User user, Pageable pageable) {
        if(Strings.isNullOrEmpty(filter)){
            return findByNotOwner(user, pageable);
        }
        else{
            return getFilteredResults(getOwnerPath().ne(user).and(QProject.project.name.containsIgnoreCase(filter)
                    .or(QProject.project.description.containsIgnoreCase(filter))), pageable);
        }
    }

}
