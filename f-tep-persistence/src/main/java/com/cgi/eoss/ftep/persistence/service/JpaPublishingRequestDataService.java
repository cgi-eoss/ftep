package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.PublishingRequest;
import com.cgi.eoss.ftep.model.QPublishingRequest;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.PublishingRequestDao;
import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaPublishingRequestDataService extends AbstractJpaDataService<PublishingRequest> implements PublishingRequestDataService {

    private final PublishingRequestDao dao;

    @Autowired
    public JpaPublishingRequestDataService(PublishingRequestDao publishingRequestDao) {
        this.dao = publishingRequestDao;
    }

    @Override
    FtepEntityDao<PublishingRequest> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(PublishingRequest entity) {
        return QPublishingRequest.publishingRequest.owner.eq(entity.getOwner())
                .and(QPublishingRequest.publishingRequest.type.eq(entity.getType()))
                .and(QPublishingRequest.publishingRequest.associatedId.eq(entity.getAssociatedId()));
    }

    @Override
    public List<PublishingRequest> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<PublishingRequest> findRequestsForPublishing(FtepService service) {
        return findOpenByAssociated(FtepService.class, service.getId());
    }

    @Override
    public List<PublishingRequest> findOpenByAssociated(Class<?> objectClass, Long identifier) {
        if (PublishingRequest.Type.of(objectClass) == null) {
            return ImmutableList.of();
        }

        return dao.findAll(QPublishingRequest.publishingRequest.type.eq(PublishingRequest.Type.of(objectClass))
                .and(QPublishingRequest.publishingRequest.associatedId.eq(identifier))
                .and(QPublishingRequest.publishingRequest.status.in(PublishingRequest.Status.REQUESTED, PublishingRequest.Status.NEEDS_INFO)));
    }

}
