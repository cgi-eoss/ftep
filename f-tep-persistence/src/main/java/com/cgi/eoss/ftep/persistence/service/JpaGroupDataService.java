package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepGroup;
import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepGroupDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepGroup.ftepGroup;

@Service
@Transactional(readOnly = true)
public class JpaGroupDataService extends AbstractJpaDataService<FtepGroup> implements GroupDataService {

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
    Predicate getUniquePredicate(FtepGroup entity) {
        return ftepGroup.name.eq(entity.getName()).and(ftepGroup.owner.eq(entity.getOwner()));
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
