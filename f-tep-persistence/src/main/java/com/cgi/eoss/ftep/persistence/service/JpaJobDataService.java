package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepJob;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepJobDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepJob.ftepJob;

@Service
@Transactional(readOnly = true)
public class JpaJobDataService extends AbstractJpaDataService<FtepJob> implements JobDataService {

    private final FtepJobDao ftepJobDao;

    private final UserDataService userDataService;

    private final ServiceDataService serviceDataService;

    @Autowired
    public JpaJobDataService(FtepJobDao ftepJobDao, UserDataService userDataService, ServiceDataService serviceDataService) {
        this.ftepJobDao = ftepJobDao;
        this.userDataService = userDataService;
        this.serviceDataService = serviceDataService;
    }

    @Override
    FtepEntityDao<FtepJob> getDao() {
        return ftepJobDao;
    }

    @Override
    Predicate getUniquePredicate(FtepJob entity) {
        return ftepJob.jobId.eq(entity.getJobId());
    }

    @Override
    public List<FtepJob> findByOwner(FtepUser user) {
        return ftepJobDao.findByOwner(user);
    }

    @Override
    public List<FtepJob> findByService(FtepService service) {
        return ftepJobDao.findByService(service);
    }

    @Override
    public List<FtepJob> findByOwnerAndService(FtepUser user, FtepService service) {
        return ftepJobDao.findByOwnerAndService(user, service);
    }

    @Override
    public FtepJob buildNew(String jobId, String ownerId, String serviceId) {
        FtepUser owner = userDataService.getByName(ownerId);
        FtepService service = serviceDataService.getByName(serviceId);

        return ftepJobDao.save(new FtepJob(jobId, owner, service));
    }

}
