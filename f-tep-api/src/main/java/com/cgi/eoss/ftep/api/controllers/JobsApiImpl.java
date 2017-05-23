package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.QJob;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.JobDao;
import com.google.common.base.Strings;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class JobsApiImpl extends BaseRepositoryApiImpl<Job> implements JobsApiCustom {

    private final FtepSecurityService securityService;
    private final JobDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QJob.job.id;
    }

    @Override
    QUser getOwnerPath() {
        return QJob.job.owner;
    }

    @Override
    Class<Job> getEntityClass() {
        return Job.class;
    }

    @Override
    public Page<Job> findByFilterOnly(String filter, Collection<Status> statuses, Pageable pageable) {
        if (statuses.isEmpty()) {
            return getDao().findAll(QJob.job.id.isNull(), pageable);
        } else {
            return getFilteredResults(getFilterExpression(filter).and(QJob.job.status.in(statuses)),
                    pageable);
        }
    }

    @Override
    public Page<Job> findByFilterAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable) {
        if (statuses.isEmpty()) {
            return getDao().findAll(QJob.job.id.isNull(), pageable);
        } else if (Strings.isNullOrEmpty(filter)) {
            return getFilteredResults(getOwnerPath().eq(user).and(QJob.job.status.in(statuses)), pageable);
        } else {
            return getFilteredResults(getOwnerPath().eq(user).and(getFilterExpression(filter))
                    .and(QJob.job.status.in(statuses)), pageable);
        }
    }

    @Override
    public Page<Job> findByFilterAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable) {
        if (statuses.isEmpty()) {
            return getDao().findAll(QJob.job.id.isNull(), pageable);
        } else if (Strings.isNullOrEmpty(filter)) {
            return getFilteredResults(getOwnerPath().ne(user).and(QJob.job.status.in(statuses)), pageable);
        } else {
            return getFilteredResults(getOwnerPath().ne(user).and(getFilterExpression(filter))
                    .and(QJob.job.status.in(statuses)), pageable);
        }
    }

    private BooleanExpression getFilterExpression(String filter) {
        return QJob.job.id.stringValue().contains(filter)
                .or(QJob.job.config.label.containsIgnoreCase(filter));
    }

}
