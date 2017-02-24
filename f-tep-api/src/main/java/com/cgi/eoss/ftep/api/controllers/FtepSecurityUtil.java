package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepEntityWithOwner;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * <p>Provides common utility-style methods for interacting with the F-TEP security context.</p>
 */
@Component
public class FtepSecurityUtil {

    private final UserDataService userDataService;

    @Autowired
    public FtepSecurityUtil(UserDataService userDataService) {
        this.userDataService = userDataService;
    }

    public void updateOwnerWithCurrentUser(FtepEntityWithOwner entity) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        entity.setOwner(userDataService.getByName(username));
    }

}
