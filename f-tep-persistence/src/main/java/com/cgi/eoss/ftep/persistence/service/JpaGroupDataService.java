package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.GroupDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QGroup.group;

@Service
@Transactional(readOnly = true)
public class JpaGroupDataService extends AbstractJpaDataService<Group> implements GroupDataService {

    private final GroupDao dao;

    @Autowired
    public JpaGroupDataService(GroupDao groupDao) {
        this.dao = groupDao;
    }

    @Override
    FtepEntityDao<Group> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(Group entity) {
        return group.name.eq(entity.getName()).and(group.owner.eq(entity.getOwner()));
    }

    @Override
    public List<Group> search(String term) {
        return dao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public Group getByName(String name) {
        return dao.findOneByName(name);
    }

    @Override
    public List<Group> findGroupMemberships(User user) {
        return dao.findByMembersContaining(user);
    }

    @Override
    public List<Group> findByOwner(User user) {
        return dao.findByOwner(user);
    }

}
