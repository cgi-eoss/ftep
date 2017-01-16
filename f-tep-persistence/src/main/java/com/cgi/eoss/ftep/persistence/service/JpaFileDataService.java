package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepFileDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaFileDataService extends AbstractJpaDataService<FtepFile> implements FileDataService {

    private static final ExampleMatcher UNIQUE_MATCHER = ExampleMatcher.matching()
            .withMatcher("name", ExampleMatcher.GenericPropertyMatcher::exact);

    private final FtepFileDao ftepFileDao;

    @Autowired
    public JpaFileDataService(FtepFileDao ftepFileDao) {
        this.ftepFileDao = ftepFileDao;
    }

    @Override
    FtepEntityDao<FtepFile> getDao() {
        return ftepFileDao;
    }

    @Override
    ExampleMatcher getUniqueMatcher() {
        return UNIQUE_MATCHER;
    }

    @Override
    public List<FtepFile> search(String term) {
        return ftepFileDao.findByNameContainingIgnoreCase(term);
    }

}
