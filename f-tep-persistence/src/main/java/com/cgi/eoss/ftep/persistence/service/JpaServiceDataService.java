package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.ServiceStatus;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepService.ftepService;

@Service
@Transactional(readOnly = true)
public class JpaServiceDataService extends AbstractJpaDataService<FtepService> implements ServiceDataService {

    private final FtepServiceDao ftepServiceDao;

    @Autowired
    public JpaServiceDataService(FtepServiceDao ftepServiceDao) {
        this.ftepServiceDao = ftepServiceDao;
    }

    @Override
    FtepEntityDao<FtepService> getDao() {
        return ftepServiceDao;
    }

    @Override
    Predicate getUniquePredicate(FtepService entity) {
        return ftepService.name.eq(entity.getName());
    }

    @Override
    public List<FtepService> search(String term) {
        return ftepServiceDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public List<FtepService> findByOwner(User user) {
        return ftepServiceDao.findByOwner(user);
    }

    @Override
    public FtepService getByName(String serviceName) {
        return ftepServiceDao.findOne(ftepService.name.eq(serviceName));
    }

    @Override
    public List<FtepService> findAllAvailable() {
        return ftepServiceDao.findByStatus(ServiceStatus.AVAILABLE);
    }

}
