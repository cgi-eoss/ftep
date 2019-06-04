package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.PublishingRequest;
import com.cgi.eoss.ftep.model.QPublishingRequest;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.persistence.dao.PublishingRequestDao;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Getter
@Component
public class PublishingRequestsApiCustomImpl extends BaseRepositoryApiImpl<PublishingRequest> implements PublishingRequestsApiCustom {

    private final FtepSecurityService securityService;
    private final PublishingRequestDao dao;

    public PublishingRequestsApiCustomImpl(FtepSecurityService securityService, PublishingRequestDao dao) {
        super(PublishingRequest.class);
        this.securityService = securityService;
        this.dao = dao;
    }

    @Override
    NumberPath<Long> getIdPath() {
        return QPublishingRequest.publishingRequest.id;
    }

    @Override
    QUser getOwnerPath() {
        return QPublishingRequest.publishingRequest.owner;
    }

    @Override
    Class<PublishingRequest> getEntityClass() {
        return PublishingRequest.class;
    }

    @Override
    public Page<PublishingRequest> findAll(Pageable pageable) {
        if (getSecurityService().isSuperUser()) {
            return getDao().findAll(pageable);
        } else {
            return getDao().findAll(getOwnerPath().eq(getSecurityService().getCurrentUser()), pageable);
        }
    }

    @Override
    public Page<PublishingRequest> searchByStatus(Collection<PublishingRequest.Status> statuses, Pageable pageable) {
        return getFilteredResults(QPublishingRequest.publishingRequest.status.in(statuses), pageable);
    }

    @Override
    Page<PublishingRequest> getFilteredResults(Predicate predicate, Pageable pageable) {
        if (getSecurityService().isSuperUser()) {
            return getDao().findAll(predicate, pageable);
        } else {
            BooleanExpression isVisible = getOwnerPath().eq(getSecurityService().getCurrentUser());
            return getDao().findAll(isVisible.and(predicate), pageable);
        }
    }
}
