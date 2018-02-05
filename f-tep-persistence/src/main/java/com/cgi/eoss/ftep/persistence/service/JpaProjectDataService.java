package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.ProjectDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QProject.project;

@Service
@Transactional(readOnly = true)
public class JpaProjectDataService extends AbstractJpaDataService<Project> implements ProjectDataService {

    private final ProjectDao dao;
    private final UserDataService userDataService;

    @Autowired
    public JpaProjectDataService(ProjectDao projectDao, UserDataService userDataService) {
        this.dao = projectDao;
        this.userDataService = userDataService;
    }

    @Override
    FtepEntityDao<Project> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(Project entity) {
        return project.name.eq(entity.getName()).and(project.owner.eq(entity.getOwner()));
    }

    @Override
    public Project refresh(Project obj) {
        obj.setOwner(userDataService.refresh(obj.getOwner()));
        return super.refresh(obj);
    }

    @Override
    public List<Project> search(String term) {
        return dao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public Project getByNameAndOwner(String name, User user) {
        return dao.findOneByNameAndOwner(name, user);
    }

    @Override
    public List<Project> findByDatabasket(Databasket databasket) {
        return dao.findByDatabasketsContaining(databasket);
    }

    @Override
    public List<Project> findByJobConfig(JobConfig jobConfig) {
        return dao.findByJobConfigsContaining(jobConfig);
    }

    @Override
    public List<Project> findByOwner(User user) {
        return dao.findByOwner(user);
    }

}
