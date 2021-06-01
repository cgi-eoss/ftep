package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.QSubscription;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.Subscription;
import com.cgi.eoss.ftep.persistence.dao.SubscriptionDao;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SubscriptionsApiCustomImpl extends BaseRepositoryApiImpl<Subscription> implements SubscriptionsApiCustom {

    private final FtepSecurityService securityService;
    private final SubscriptionDao dao;

    public SubscriptionsApiCustomImpl(FtepSecurityService securityService, SubscriptionDao subscriptionDao) {
        super(Subscription.class);
        this.securityService = securityService;
        this.dao = subscriptionDao;
    }

    @Override
    NumberPath<Long> getIdPath() {
        return QSubscription.subscription.id;
    }

    @Override
    QUser getOwnerPath() {
        return QSubscription.subscription.owner;
    }

    @Override
    Class<Subscription> getEntityClass() {
        return Subscription.class;
    }
}
