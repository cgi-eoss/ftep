package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepDatasource;
import com.cgi.eoss.ftep.persistence.dao.FtepDatasourceDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaDatasourceDataService extends AbstractJpaDataService<FtepDatasource> implements DatasourceDataService {

    private static final ExampleMatcher UNIQUE_MATCHER = ExampleMatcher.matching()
            .withMatcher("name", ExampleMatcher.GenericPropertyMatcher::exact);

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
    ExampleMatcher getUniqueMatcher() {
        return UNIQUE_MATCHER;
    }

    @Override
    public List<FtepDatasource> search(String term) {
        return ftepDatasourceDao.findByNameContainingIgnoreCase(term);
    }

}
