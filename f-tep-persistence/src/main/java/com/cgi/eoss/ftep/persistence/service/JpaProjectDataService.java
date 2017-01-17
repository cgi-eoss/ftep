package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepProject;
import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepProjectDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepProject.ftepProject;

@Service
@Transactional(readOnly = true)
public class JpaProjectDataService extends AbstractJpaDataService<FtepProject> implements ProjectDataService {

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
    Predicate getUniquePredicate(FtepProject entity) {
        return ftepProject.name.eq(entity.getName()).and(ftepProject.owner.eq(entity.getOwner()));
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
