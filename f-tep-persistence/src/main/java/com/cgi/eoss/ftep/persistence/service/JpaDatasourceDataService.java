package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepDatasource;
import com.cgi.eoss.ftep.persistence.dao.FtepDatasourceDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepDatasource.ftepDatasource;

@Service
@Transactional(readOnly = true)
public class JpaDatasourceDataService extends AbstractJpaDataService<FtepDatasource> implements DatasourceDataService {

    private final FtepDatasourceDao ftepDatasourceDao;

    @Autowired
    public JpaDatasourceDataService(FtepDatasourceDao ftepDatasourceDao) {
        this.ftepDatasourceDao = ftepDatasourceDao;
    }

    @Override
    FtepEntityDao<FtepDatasource> getDao() {
        return ftepDatasourceDao;
    }

    @Override
    Predicate getUniquePredicate(FtepDatasource entity) {
        return ftepDatasource.name.eq(entity.getName());
    }

    @Override
    public List<FtepDatasource> search(String term) {
        return ftepDatasourceDao.findByNameContainingIgnoreCase(term);
    }

}
