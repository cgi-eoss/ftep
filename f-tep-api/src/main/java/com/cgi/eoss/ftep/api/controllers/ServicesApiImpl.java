package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class ServicesApiImpl implements ServicesApiInferringOwner {

    private final FtepSecurityService ftepSecurityService;
    private final FtepServiceDao ftepServiceDao;

    @Autowired
    public ServicesApiImpl(FtepSecurityService ftepSecurityService, FtepServiceDao ftepServiceDao) {
        this.ftepSecurityService = ftepSecurityService;
        this.ftepServiceDao = ftepServiceDao;
    }

    @Override
    public <S extends FtepService> S save(S ftepService) {
        if (ftepService.getOwner() == null) {
            ftepSecurityService.updateOwnerWithCurrentUser(ftepService);
        }
        return ftepServiceDao.save(ftepService);
    }
}
