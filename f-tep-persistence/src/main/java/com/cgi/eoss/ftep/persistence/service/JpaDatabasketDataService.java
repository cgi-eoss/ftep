package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepDatabasket;
import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.dao.FtepDatabasketDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaDatabasketDataService extends AbstractJpaDataService<FtepDatabasket> implements DatabasketDataService {

    private static final ExampleMatcher UNIQUE_MATCHER = ExampleMatcher.matching()
            .withMatcher("name", ExampleMatcher.GenericPropertyMatcher::exact)
            .withMatcher("owner", ExampleMatcher.GenericPropertyMatcher::exact);

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
    ExampleMatcher getUniqueMatcher() {
        return UNIQUE_MATCHER;
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
