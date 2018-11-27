package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.SystematicProcessing;
import com.cgi.eoss.ftep.model.SystematicProcessing.Status;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.SystematicProcessingDao;

import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QSystematicProcessing.systematicProcessing;

@Service
@Transactional(readOnly = true)
public class JpaSystematicProcessingDataService extends AbstractJpaDataService<SystematicProcessing> implements SystematicProcessingDataService {

    private final SystematicProcessingDao dao;

    @Autowired
    public JpaSystematicProcessingDataService(SystematicProcessingDao systematicProcessingDao) {
        this.dao = systematicProcessingDao;
    }

    @Override
    public FtepEntityDao<SystematicProcessing> getDao() {
        return dao;
    }

    @Override
    public Predicate getUniquePredicate(SystematicProcessing entity) {
        return systematicProcessing.id.eq(entity.getId());
    }

    @Override
    public List<SystematicProcessing> findByStatus(Status s) {
        return dao.findAll(systematicProcessing.status.eq(s));
    }
}
