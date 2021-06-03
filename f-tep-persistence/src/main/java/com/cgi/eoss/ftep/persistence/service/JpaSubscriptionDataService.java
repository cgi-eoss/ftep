package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Subscription;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.SubscriptionDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QSubscription.subscription;

@Service
@Transactional(readOnly = true)
public class JpaSubscriptionDataService extends AbstractJpaDataService<Subscription> implements SubscriptionDataService {

    private final SubscriptionDao subscriptionDao;

    @Autowired
    public JpaSubscriptionDataService(SubscriptionDao subscriptionDao) {
        this.subscriptionDao = subscriptionDao;
    }

    @Override
    FtepEntityDao<Subscription> getDao() {
        return subscriptionDao;
    }

    @Override
    Predicate getUniquePredicate(Subscription entity) {
        return subscription.owner.eq(entity.getOwner()).and(subscription.creationTime.eq(entity.getCreationTime()));
    }

    @Override
    public List<Subscription> findByOwner(User user) {
        return subscriptionDao.findByOwner(user);
    }
}
