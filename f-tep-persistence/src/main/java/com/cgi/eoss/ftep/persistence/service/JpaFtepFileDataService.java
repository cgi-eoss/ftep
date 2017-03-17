package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepFileType;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepFileDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.cgi.eoss.ftep.model.QFtepFile.ftepFile;

@Service
@Transactional(readOnly = true)
public class JpaFtepFileDataService extends AbstractJpaDataService<FtepFile> implements FtepFileDataService {

    private final FtepFileDao dao;

    @Autowired
    public JpaFtepFileDataService(FtepFileDao ftepFileDao) {
        this.dao = ftepFileDao;
    }

    @Override
    FtepEntityDao<FtepFile> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(FtepFile entity) {
        return ftepFile.uri.eq(entity.getUri()).or(ftepFile.restoId.eq(entity.getRestoId()));
    }

    @Override
    public FtepFile getByUri(URI uri) {
        return dao.findOneByUri(uri);
    }

    @Override
    public FtepFile getByRestoId(UUID uuid) {
        return dao.findOneByRestoId(uuid);
    }

    @Override
    public List<FtepFile> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<FtepFile> getByType(FtepFileType type) {
        return dao.findByType(type);
    }

}
