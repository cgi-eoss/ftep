package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.PublishingRequest;
import com.cgi.eoss.ftep.model.QPublishingRequest;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.PublishingRequestDao;
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
        return dao.findAll(QPublishingRequest.publishingRequest.type.eq(PublishingRequest.Type.SERVICE)
                .and(QPublishingRequest.publishingRequest.associatedId.eq(service.getId())));
    }

}
