package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.QJob;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.JobDao;
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
        return getFilteredResults(getFilterPredicate(filter, statuses), pageable);
    }

    @Override
    public Page<Job> findByFilterAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, statuses)), pageable);
    }

    @Override
    public Page<Job> findByFilterAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter, statuses)), pageable);
    }

    private Predicate getFilterPredicate(String filter, Collection<Status> statuses) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QJob.job.id.stringValue().contains(filter)
                    .or(QJob.job.config.label.containsIgnoreCase(filter))
                    .or(QJob.job.config.service.name.containsIgnoreCase(filter)));
        }

        if (!statuses.isEmpty()) {
            builder.and(QJob.job.status.in(statuses));
        }

        return builder.getValue();
    }

}
