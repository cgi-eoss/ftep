package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepUserDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepUser.ftepUser;

@Service
@Transactional(readOnly = true)
public class JpaUserDataService extends AbstractJpaDataService<FtepUser> implements UserDataService {

    private static final ExampleMatcher UNIQUE_MATCHER = ExampleMatcher.matching()
            .withMatcher("name", ExampleMatcher.GenericPropertyMatcher::exact);

    private final FtepUserDao ftepUserDao;

    @Autowired
    public JpaUserDataService(FtepUserDao ftepUserDao) {
        this.ftepUserDao = ftepUserDao;
    }

    @Override
    FtepEntityDao<FtepUser> getDao() {
        return ftepUserDao;
    }

    @Override
    Predicate getUniquePredicate(FtepUser entity) {
        return ftepUser.name.eq(entity.getName());
    }

    @Override
    public List<FtepUser> search(String term) {
        return ftepUserDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public FtepUser getByName(String name) {
        return ftepUserDao.findOne(ftepUser.name.eq(name));
    }
}
