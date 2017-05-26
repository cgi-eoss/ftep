package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.PublishingRequest;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface PublishingRequestDao extends FtepEntityDao<PublishingRequest> {
    List<PublishingRequest> findByOwner(User user);
}
