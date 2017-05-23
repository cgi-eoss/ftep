package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.QFtepService;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceDao;
import com.google.common.base.Strings;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ServicesApiImpl extends BaseRepositoryApiImpl<FtepService> implements ServicesApiCustom {

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

    @Override
    public Page<FtepService> findByFilterOnly(String filter, Pageable pageable) {
        return getFilteredResults(QFtepService.ftepService.name.containsIgnoreCase(filter)
                .or(QFtepService.ftepService.description.containsIgnoreCase(filter)), pageable);
    }

    @Override
    public Page<FtepService> findByFilterAndOwner(String filter, User user, Pageable pageable) {
        if (Strings.isNullOrEmpty(filter)) {
            return findByOwner(user, pageable);
        } else {
            return getFilteredResults(getOwnerPath().eq(user).and(QFtepService.ftepService.name.containsIgnoreCase(filter)
                    .or(QFtepService.ftepService.description.containsIgnoreCase(filter))), pageable);
        }
    }

    @Override
    public Page<FtepService> findByFilterAndNotOwner(String filter, User user, Pageable pageable) {
        if (Strings.isNullOrEmpty(filter)) {
            return findByNotOwner(user, pageable);
        } else {
            return getFilteredResults(getOwnerPath().ne(user).and(QFtepService.ftepService.name.containsIgnoreCase(filter)
                    .or(QFtepService.ftepService.description.containsIgnoreCase(filter))), pageable);
        }
    }

}
