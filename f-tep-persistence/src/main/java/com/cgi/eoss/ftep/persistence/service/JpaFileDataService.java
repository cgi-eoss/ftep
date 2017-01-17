package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepFileDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepFile.ftepFile;

@Service
@Transactional(readOnly = true)
public class JpaFileDataService extends AbstractJpaDataService<FtepFile> implements FileDataService {

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
    Predicate getUniquePredicate(FtepFile entity) {
        return ftepFile.name.eq(entity.getName());
    }

    @Override
    public List<FtepFile> search(String term) {
        return ftepFileDao.findByNameContainingIgnoreCase(term);
    }

}
