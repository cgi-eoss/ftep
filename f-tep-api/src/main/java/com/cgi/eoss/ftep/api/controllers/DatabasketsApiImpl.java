package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.QDatabasket;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.persistence.dao.DatabasketDao;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class DatabasketsApiImpl extends BaseRepositoryApiImpl<Databasket> {

    private final FtepSecurityService securityService;
    private final DatabasketDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QDatabasket.databasket.id;
    }

    @Override
    QUser getOwnerPath() {
        return QDatabasket.databasket.owner;
    }

    @Override
    Class<Databasket> getEntityClass() {
        return Databasket.class;
    }

}
