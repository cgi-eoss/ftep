package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.JobConfigDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QJobConfig.jobConfig;

@Service
@Transactional(readOnly = true)
public class JpaJobConfigDataService extends AbstractJpaDataService<JobConfig> implements JobConfigDataService {

    private final JobConfigDao dao;

    @Autowired
    public JpaJobConfigDataService(JobConfigDao jobConfigDao, UserDataService userDataService, ServiceDataService serviceDataService) {
        this.dao = jobConfigDao;
    }

    @Override
    FtepEntityDao<JobConfig> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(JobConfig entity) {
        return jobConfig.owner.eq(entity.getOwner())
                .and(jobConfig.service.eq(entity.getService()))
                .and(jobConfig.inputs.eq(entity.getInputs()));
    }

    @Override
    public List<JobConfig> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<JobConfig> findByService(FtepService service) {
        return dao.findByService(service);
    }

    @Override
    public List<JobConfig> findByOwnerAndService(User user, FtepService service) {
        return dao.findByOwnerAndService(user, service);
    }

}
