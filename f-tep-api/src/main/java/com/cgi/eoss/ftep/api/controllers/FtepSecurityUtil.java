package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepEntityWithOwner;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

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
        entity.setOwner(getCurrentUser());
    }

    public User getCurrentUser() {
        String username = getCurrentAuthentication().getName();
        return userDataService.getByName(username);
    }

    public Collection<? extends GrantedAuthority> getCurrentAuthorities() {
        return getCurrentAuthentication().getAuthorities();
    }

    public Set<Group> getCurrentGroups() {
        return userDataService.getGroups(getCurrentUser());
    }

    private Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

}
