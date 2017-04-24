package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.DataSource;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.DataSourceDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QDataSource.dataSource;

@Service
@Transactional(readOnly = true)
public class JpaDataSourceDataService extends AbstractJpaDataService<DataSource> implements DataSourceDataService {

    private final DataSourceDao dao;

    @Autowired
    public JpaDataSourceDataService(DataSourceDao dataSourceDao) {
        this.dao = dataSourceDao;
    }

    @Override
    FtepEntityDao<DataSource> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(DataSource entity) {
        return dataSource.name.eq(entity.getName());
    }

    @Override
    public List<DataSource> search(String term) {
        return dao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public DataSource getByName(String name) {
        return dao.findOneByName(name);
    }

    @Override
    public List<DataSource> findByOwner(User user) {
        return dao.findByOwner(user);
    }

}
