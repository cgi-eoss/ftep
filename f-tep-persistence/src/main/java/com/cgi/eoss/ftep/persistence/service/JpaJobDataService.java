package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepJob;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepJobDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaJobDataService extends AbstractJpaDataService<FtepJob> implements JobDataService {

    private static final ExampleMatcher UNIQUE_MATCHER = ExampleMatcher.matching()
            .withMatcher("jobId", ExampleMatcher.GenericPropertyMatcher::exact);

    private final FtepJobDao ftepJobDao;

    @Autowired
    public JpaJobDataService(FtepJobDao ftepJobDao) {
        this.ftepJobDao = ftepJobDao;
    }

    @Override
    FtepEntityDao<FtepJob> getDao() {
        return ftepJobDao;
    }

    @Override
    ExampleMatcher getUniqueMatcher() {
        return UNIQUE_MATCHER;
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

}
