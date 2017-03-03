package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.persistence.dao.DatabasketDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class DatabasketsApiImpl implements DatabasketsApiInferringOwner {

    private final FtepSecurityUtil ftepSecurityUtil;
    private final DatabasketDao databasketDao;

    @Autowired
    public DatabasketsApiImpl(FtepSecurityUtil ftepSecurityUtil, DatabasketDao databasketDao) {
        this.ftepSecurityUtil = ftepSecurityUtil;
        this.databasketDao = databasketDao;
    }

    @Override
    public <S extends Databasket> S save(S databasket) {
        if (databasket.getOwner() == null) {
            ftepSecurityUtil.updateOwnerWithCurrentUser(databasket);
        }
        return databasketDao.save(databasket);
    }

}
