package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.QDatabasket;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.DatabasketDao;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class DatabasketsApiImpl extends BaseRepositoryApiImpl<Databasket> implements DatabasketsApiCustom {

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

    @Override
    public Page<Databasket> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(@Param("filter") String filter,
            Pageable pageable) {
        return getFilteredResults(QDatabasket.databasket.name.containsIgnoreCase(filter)
                .or(QDatabasket.databasket.description.containsIgnoreCase(filter)), pageable);
    }

    @Override
    public Page<Databasket> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndOwner(
            @Param("filter") String filter, @Param("owner") User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(QDatabasket.databasket.name.containsIgnoreCase(filter)
                .or(QDatabasket.databasket.description.containsIgnoreCase(filter))), pageable);
    }

    @Override
    public Page<Databasket> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndNotOwner(
            @Param("filter") String filter, @Param("owner") User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(QDatabasket.databasket.name.containsIgnoreCase(filter)
                .or(QDatabasket.databasket.description.containsIgnoreCase(filter))), pageable);
    }

}
