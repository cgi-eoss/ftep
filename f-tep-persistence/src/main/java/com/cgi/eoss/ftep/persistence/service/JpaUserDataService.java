package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.UserDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QUser.user;

@Service
@Transactional(readOnly = true)
public class JpaUserDataService extends AbstractJpaDataService<User> implements UserDataService {

    private final UserDao dao;

    @Autowired
    public JpaUserDataService(UserDao userDao) {
        this.dao = userDao;
    }

    @Override
    FtepEntityDao<User> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(User entity) {
        return user.name.eq(entity.getName());
    }

    @Override
    public List<User> search(String term) {
        return dao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public User getByName(String name) {
        return dao.findOne(user.name.eq(name));
    }

}
