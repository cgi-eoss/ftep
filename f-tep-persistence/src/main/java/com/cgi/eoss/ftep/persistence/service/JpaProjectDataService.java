package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepProject;
import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepProjectDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaProjectDataService extends AbstractJpaDataService<FtepProject> implements ProjectDataService {

    private static final ExampleMatcher UNIQUE_MATCHER = ExampleMatcher.matching()
            .withMatcher("name", ExampleMatcher.GenericPropertyMatcher::exact)
            .withMatcher("owner", ExampleMatcher.GenericPropertyMatcher::exact);

    private final FtepProjectDao ftepProjectDao;

    @Autowired
    public JpaProjectDataService(FtepProjectDao ftepProjectDao) {
        this.ftepProjectDao = ftepProjectDao;
    }

    @Override
    FtepEntityDao<FtepProject> getDao() {
        return ftepProjectDao;
    }

    @Override
    ExampleMatcher getUniqueMatcher() {
        return UNIQUE_MATCHER;
    }

    @Override
    public List<FtepProject> search(String term) {
        return ftepProjectDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public List<FtepProject> findByOwner(FtepUser user) {
        return ftepProjectDao.findByOwner(user);
    }

}
