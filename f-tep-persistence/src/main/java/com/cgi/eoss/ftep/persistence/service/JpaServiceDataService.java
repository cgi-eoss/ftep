package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaServiceDataService extends AbstractJpaDataService<FtepService> implements ServiceDataService {

    private static final ExampleMatcher UNIQUE_MATCHER = ExampleMatcher.matching()
            .withMatcher("name", ExampleMatcher.GenericPropertyMatcher::exact)
            .withMatcher("owner", ExampleMatcher.GenericPropertyMatcher::exact);

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
    ExampleMatcher getUniqueMatcher() {
        return UNIQUE_MATCHER;
    }

    @Override
    public List<FtepService> search(String term) {
        return ftepServiceDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public List<FtepService> findByOwner(FtepUser user) {
        return ftepServiceDao.findByOwner(user);
    }

}
