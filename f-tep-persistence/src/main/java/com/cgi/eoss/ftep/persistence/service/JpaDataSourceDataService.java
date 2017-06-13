package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.DataSource;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.DataSourceDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.cgi.eoss.ftep.model.QDataSource.dataSource;

@Service
@Transactional(readOnly = true)
public class JpaDataSourceDataService extends AbstractJpaDataService<DataSource> implements DataSourceDataService {

    private final DataSourceDao dao;
    private final UserDataService userDataService;

    @Autowired
    public JpaDataSourceDataService(DataSourceDao dataSourceDao, UserDataService userDataService) {
        this.dao = dataSourceDao;
        this.userDataService = userDataService;
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

    @Transactional
    @Override
    public DataSource getForService(FtepService service) {
        return getOrCreate(service.getDataSourceName());
    }

    @Transactional
    @Override
    public DataSource getForExternalProduct(FtepFile ftepFile) {
        return getOrCreate(ftepFile.getUri().getScheme());
    }

    @Transactional
    @Override
    public DataSource getForRefData(FtepFile ftepFile) {
        return getOrCreate(ftepFile.getUri().getScheme());
    }

    private DataSource getOrCreate(String name) {
        return maybeGetByName(name).orElseGet(() -> save(new DataSource(name, userDataService.getDefaultUser())));
    }

    private Optional<DataSource> maybeGetByName(String name) {
        return Optional.ofNullable(dao.findOneByName(name));
    }

}
