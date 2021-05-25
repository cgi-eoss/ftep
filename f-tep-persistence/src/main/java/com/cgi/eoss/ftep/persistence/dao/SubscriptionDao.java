package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.Subscription;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface SubscriptionDao extends FtepEntityDao<Subscription> {
    List<Subscription> findByOwner(User user);
}
