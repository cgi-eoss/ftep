package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.persistence.dao.GroupDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class GroupsApiImpl implements GroupsApiInferringOwner {

    private final FtepSecurityUtil ftepSecurityUtil;
    private final GroupDao groupDao;

    @Autowired
    public GroupsApiImpl(FtepSecurityUtil ftepSecurityUtil, GroupDao groupDao) {
        this.ftepSecurityUtil = ftepSecurityUtil;
        this.groupDao = groupDao;
    }

    @Override
    public <S extends Group> S save(S group) {
        if (group.getOwner() == null) {
            ftepSecurityUtil.updateOwnerWithCurrentUser(group);
        }
        return groupDao.save(group);
    }

}
