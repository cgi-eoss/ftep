package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.QFtepService;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceDao;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
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
    public Page<FtepService> findByFilterOnly(String filter, FtepService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, serviceType), pageable);
    }

    @Override
    public Page<FtepService> findByFilterAndOwner(String filter, User user, FtepService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, serviceType)), pageable);
    }

    @Override
    public Page<FtepService> findByFilterAndNotOwner(String filter, User user, FtepService.Type serviceType, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user).and(getFilterPredicate(filter, serviceType)), pageable);
    }

    private Predicate getFilterPredicate(String filter, FtepService.Type serviceType) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QFtepService.ftepService.name.containsIgnoreCase(filter).or(QFtepService.ftepService.description.containsIgnoreCase(filter)));
        }

        if (serviceType != null) {
            builder.and(QFtepService.ftepService.type.eq(serviceType));
        }

        return builder.getValue();
    }

}
