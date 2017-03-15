package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceContextFileDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepServiceContextFile.ftepServiceContextFile;

@Service
@Transactional(readOnly = true)
public class JpaServiceFileDataService extends AbstractJpaDataService<FtepServiceContextFile> implements ServiceFileDataService {

    private final FtepServiceContextFileDao ftepServiceContextFileDao;

    @Autowired
    public JpaServiceFileDataService(FtepServiceContextFileDao ftepServiceContextFileDao) {
        this.ftepServiceContextFileDao = ftepServiceContextFileDao;
    }

    @Override
    FtepEntityDao<FtepServiceContextFile> getDao() {
        return ftepServiceContextFileDao;
    }

    @Override
    Predicate getUniquePredicate(FtepServiceContextFile entity) {
        return ftepServiceContextFile.service.eq(entity.getService()).and(ftepServiceContextFile.filename.eq(entity.getFilename()));
    }

    @Override
    public List<FtepServiceContextFile> findByService(FtepService service) {
        return ftepServiceContextFileDao.findByService(service);
    }

}
