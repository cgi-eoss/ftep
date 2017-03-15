package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.persistence.dao.DatabasketDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class DatabasketsApiImpl implements DatabasketsApiInferringOwner {

    private final FtepSecurityService ftepSecurityService;
    private final DatabasketDao databasketDao;

    @Autowired
    public DatabasketsApiImpl(FtepSecurityService ftepSecurityService, DatabasketDao databasketDao) {
        this.ftepSecurityService = ftepSecurityService;
        this.databasketDao = databasketDao;
    }

    @Override
    public <S extends Databasket> S save(S databasket) {
        if (databasket.getOwner() == null) {
            ftepSecurityService.updateOwnerWithCurrentUser(databasket);
        }
        return databasketDao.save(databasket);
    }

}
