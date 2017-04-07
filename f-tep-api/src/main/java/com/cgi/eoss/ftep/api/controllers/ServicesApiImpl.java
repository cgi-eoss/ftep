package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.QFtepService;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceDao;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ServicesApiImpl extends BaseRepositoryApiImpl<FtepService> {

    private final FtepSecurityService securityService;
    private final FtepServiceDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QFtepService.ftepService.id;
    }

    @Override
    QUser getOwnerPath() {
        return QFtepService.ftepService.owner;
    }

    @Override
    Class<FtepService> getEntityClass() {
        return FtepService.class;
    }

}
