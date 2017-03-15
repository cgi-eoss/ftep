package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.persistence.dao.ProjectDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class ProjectsApiImpl implements ProjectsApiInferringOwner {

    private final FtepSecurityService ftepSecurityService;
    private final ProjectDao projectDao;

    @Autowired
    public ProjectsApiImpl(FtepSecurityService ftepSecurityService, ProjectDao projectDao) {
        this.ftepSecurityService = ftepSecurityService;
        this.projectDao = projectDao;
    }

    @Override
    public <S extends Project> S save(S project) {
        if (project.getOwner() == null) {
            ftepSecurityService.updateOwnerWithCurrentUser(project);
        }
        return projectDao.save(project);
    }

}
