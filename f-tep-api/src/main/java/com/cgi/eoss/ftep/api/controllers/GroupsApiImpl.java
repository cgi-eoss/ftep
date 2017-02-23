package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.persistence.dao.GroupDao;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class GroupsApiImpl implements GroupsApiInferringOwner {

    private final GroupDao groupDao;
    private final UserDataService userDataService;

    @Autowired
    public GroupsApiImpl(GroupDao groupDao, UserDataService userDataService) {
        this.groupDao = groupDao;
        this.userDataService = userDataService;
    }

    @Override
    public <S extends Group> S save(S group) {
        if (group.getOwner() == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                String username = ((User)authentication.getPrincipal()).getUsername();
                group.setOwner(userDataService.getByName(username));
            }
        }
        return groupDao.save(group);
    }
}
