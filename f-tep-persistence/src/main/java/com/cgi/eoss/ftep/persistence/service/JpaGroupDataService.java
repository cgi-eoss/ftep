package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepGroup;
import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepGroupDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaGroupDataService extends AbstractJpaDataService<FtepGroup> implements GroupDataService {

    private static final ExampleMatcher UNIQUE_MATCHER = ExampleMatcher.matching()
            .withMatcher("name", ExampleMatcher.GenericPropertyMatcher::exact)
            .withMatcher("owner", ExampleMatcher.GenericPropertyMatcher::exact);

    private final FtepGroupDao ftepGroupDao;

    @Autowired
    public JpaGroupDataService(FtepGroupDao ftepGroupDao) {
        this.ftepGroupDao = ftepGroupDao;
    }

    @Override
    FtepEntityDao<FtepGroup> getDao() {
        return ftepGroupDao;
    }

    @Override
    ExampleMatcher getUniqueMatcher() {
        return UNIQUE_MATCHER;
    }

    @Override
    public List<FtepGroup> search(String term) {
        return ftepGroupDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public List<FtepGroup> findGroupMemberships(FtepUser user) {
        return ftepGroupDao.findByMembersContaining(user);
    }

    @Override
    public List<FtepGroup> findByOwner(FtepUser user) {
        return ftepGroupDao.findByOwner(user);
    }

}
