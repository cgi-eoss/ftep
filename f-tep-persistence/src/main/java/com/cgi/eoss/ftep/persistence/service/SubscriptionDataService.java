package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Subscription;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface SubscriptionDataService extends FtepEntityDataService<Subscription> {
    List<Subscription> findByOwner(User user);
}
