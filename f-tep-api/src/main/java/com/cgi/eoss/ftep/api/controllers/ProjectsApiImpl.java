package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.persistence.dao.ProjectDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class ProjectsApiImpl implements ProjectsApiInferringOwner {

    private final FtepSecurityUtil ftepSecurityUtil;
    private final ProjectDao projectDao;

    @Autowired
    public ProjectsApiImpl(FtepSecurityUtil ftepSecurityUtil, ProjectDao projectDao) {
        this.ftepSecurityUtil = ftepSecurityUtil;
        this.projectDao = projectDao;
    }

    @Override
    public <S extends Project> S save(S project) {
        if (project.getOwner() == null) {
            ftepSecurityUtil.updateOwnerWithCurrentUser(project);
        }
        return projectDao.save(project);
    }

}
