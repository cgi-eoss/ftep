package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.PublishingRequest;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface PublishingRequestDataService extends
        FtepEntityDataService<PublishingRequest> {
    List<PublishingRequest> findByOwner(User user);

    List<PublishingRequest> findRequestsForPublishing(FtepService service);

    List<PublishingRequest> findOpenByAssociated(Class<?> objectClass, Long identifier);
}
