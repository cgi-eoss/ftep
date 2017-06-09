package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.QProject;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.ProjectDao;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
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
    public Page<Project> findByFilterOnly(String filter, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter), pageable);
    }

    @Override
    public Page<Project> findByFilterAndOwner(String filter, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter)), pageable);
    }

    @Override
    public Page<Project> findByFilterAndNotOwner(String filter, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter)), pageable);
    }

    private Predicate getFilterPredicate(String filter) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QProject.project.name.containsIgnoreCase(filter)
                    .or(QProject.project.description.containsIgnoreCase(filter)));
        }

        return builder.getValue();
    }

}
