package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.DatabasketDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QDatabasket.databasket;

@Service
@Transactional(readOnly = true)
public class JpaDatabasketDataService extends AbstractJpaDataService<Databasket> implements DatabasketDataService {

    private final DatabasketDao dao;

    @Autowired
    public JpaDatabasketDataService(DatabasketDao databasketDao) {
        this.dao = databasketDao;
    }

    @Override
    FtepEntityDao<Databasket> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(Databasket entity) {
        return databasket.name.eq(entity.getName()).and(databasket.owner.eq(entity.getOwner()));
    }

    @Override
    public List<Databasket> search(String term) {
        return dao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public Databasket getByNameAndOwner(String name, User user) {
        return dao.findOneByNameAndOwner(name, user);
    }

    @Override
    public List<Databasket> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<Databasket> findByFiles(FtepFile file) {
        return dao.findByFiles(file);
    }


    @Override
    public List<Databasket> findByFile(FtepFile file) {
        return dao.findByFilesContaining(file);
    }

}
