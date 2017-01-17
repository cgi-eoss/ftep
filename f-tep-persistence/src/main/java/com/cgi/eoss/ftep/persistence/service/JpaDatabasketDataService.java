package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepDatabasket;
import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.dao.FtepDatabasketDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepDatabasket.ftepDatabasket;

@Service
@Transactional(readOnly = true)
public class JpaDatabasketDataService extends AbstractJpaDataService<FtepDatabasket> implements DatabasketDataService {

    private final FtepDatabasketDao ftepDatabasketDao;

    @Autowired
    public JpaDatabasketDataService(FtepDatabasketDao ftepDatabasketDao) {
        this.ftepDatabasketDao = ftepDatabasketDao;
    }

    @Override
    FtepEntityDao<FtepDatabasket> getDao() {
        return ftepDatabasketDao;
    }

    @Override
    Predicate getUniquePredicate(FtepDatabasket entity) {
        return ftepDatabasket.name.eq(entity.getName()).and(ftepDatabasket.owner.eq(entity.getOwner()));
    }

    @Override
    public List<FtepDatabasket> search(String term) {
        return ftepDatabasketDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public List<FtepDatabasket> findByOwner(FtepUser user) {
        return ftepDatabasketDao.findByOwner(user);
    }

}
