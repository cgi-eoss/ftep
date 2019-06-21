package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.QSystematicProcessing;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.SystematicProcessing;
import com.cgi.eoss.ftep.persistence.dao.SystematicProcessingDao;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class SystematicProcessingsApiCustomImpl extends BaseRepositoryApiImpl<SystematicProcessing> implements SystematicProcessingsApiCustom {

    private final FtepSecurityService securityService;
    private final SystematicProcessingDao dao;

    public SystematicProcessingsApiCustomImpl(FtepSecurityService securityService, SystematicProcessingDao dao) {
        super(SystematicProcessing.class);
        this.securityService = securityService;
        this.dao = dao;
    }

    @Override
    NumberPath<Long> getIdPath() {
        return QSystematicProcessing.systematicProcessing.id;
    }

    @Override
    QUser getOwnerPath() {
        return QSystematicProcessing.systematicProcessing.owner;
    }

    @Override
    Class<SystematicProcessing> getEntityClass() {
        return SystematicProcessing.class;
    }

}
