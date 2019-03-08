package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.QJob;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.JobDao;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
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
public class JobsApiCustomImpl extends BaseRepositoryApiImpl<Job> implements JobsApiCustom {

    private final FtepSecurityService securityService;
    private final JobDao dao;

    @Override
    public NumberPath<Long> getIdPath() {
        return QJob.job.id;
    }

    @Override
    public QUser getOwnerPath() {
        return QJob.job.owner;
    }

    @Override
    public Class<Job> getEntityClass() {
        return Job.class;
    }

    public BooleanExpression isNotSubjob() {
        return QJob.job.parentJob.isNull();
    }

    public BooleanExpression isChildOf(Long parentId) {
        return QJob.job.parentJob.id.eq(parentId);
    }

    // --- Filter

    @Override
    public Page<Job> searchByFilterOnly(String filter, Collection<Status> statuses, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, statuses), pageable);
    }

    @Override
    public Page<Job> searchByFilterAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, statuses)), pageable);
    }

    @Override
    public Page<Job> searchByFilterAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter, statuses)), pageable);
    }

    // --- Filter / Not Subjob

    @Override
    public Page<Job> searchByFilterAndIsNotSubjob(String filter, Collection<Status> statuses, Pageable pageable) {
        return getFilteredResults(isNotSubjob().and(getFilterPredicate(filter, statuses)), pageable);
    }

    @Override
    public Page<Job> searchByFilterAndIsNotSubjobAndOwner(String filter, Collection<Status> statuses, User user, Pageable pageable) {
        return getFilteredResults(isNotSubjob().and(getOwnerPath().eq(user)).and(getFilterPredicate(filter, statuses)), pageable);
    }

    @Override
    public Page<Job> searchByFilterAndIsNotSubjobAndNotOwner(String filter, Collection<Status> statuses, User user, Pageable pageable) {
        return getFilteredResults(isNotSubjob().and(getOwnerPath().ne(user)).and(getFilterPredicate(filter, statuses)), pageable);
    }

    // --- Filter / Parent

    @Override
    public Page<Job> searchByFilterAndParent(String filter, Collection<Status> statuses, Long parentId, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, statuses, parentId), pageable);
    }

    @Override
    public Page<Job> searchByFilterAndParentAndOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable) {
        return getFilteredResults((getOwnerPath().eq(user)).and(getFilterPredicate(filter, statuses, parentId)), pageable);
    }

    @Override
    public Page<Job> searchByFilterAndParentAndNotOwner(String filter, Collection<Status> statuses, Long parentId, User user, Pageable pageable) {
        return getFilteredResults((getOwnerPath().ne(user)).and(getFilterPredicate(filter, statuses, parentId)), pageable);
    }

    // --- Filter Predicate

    public Predicate getFilterPredicate(String filter, Collection<Status> statuses) {
        return getFilterPredicate(filter, statuses, null);
    }

    public Predicate getFilterPredicate(String filter, Collection<Status> statuses, Long parentId) {
        BooleanBuilder builder = new BooleanBuilder(Expressions.asBoolean(true).isTrue());

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QJob.job.id.stringValue().contains(filter)
                    .or(QJob.job.config.label.containsIgnoreCase(filter))
                    .or(QJob.job.config.service.name.containsIgnoreCase(filter)));
        }
        if (statuses != null && !statuses.isEmpty()) {
            builder.and(QJob.job.status.in(statuses));
        }
        if (parentId != null) {
            builder.and(isChildOf(parentId));
        }

        return builder.getValue();
    }
}
