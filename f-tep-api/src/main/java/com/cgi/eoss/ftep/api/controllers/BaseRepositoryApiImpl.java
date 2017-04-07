package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepEntityWithOwner;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public abstract class BaseRepositoryApiImpl<T extends FtepEntityWithOwner<T>> implements BaseRepositoryApi<T> {

    @Override
    public <S extends T> S save(S entity) {
        if (entity.getOwner() == null) {
            getSecurityService().updateOwnerWithCurrentUser(entity);
        }
        return getDao().save(entity);
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        if (getSecurityService().isSuperUser()) {
            return getDao().findAll(pageable);
        } else {
            Set<Long> visibleIds = getSecurityService().getVisibleObjectIds(getEntityClass(), getDao().findAllIds());
            BooleanExpression isVisible = getIdPath().in(visibleIds);
            return getDao().findAll(isVisible, pageable);
        }
    }

    @Override
    public Page<T> findByOwner(User user, Pageable pageable) {
        BooleanExpression hasOwner = getOwnerPath().eq(user);
        if (getSecurityService().isSuperUser()) {
            return getDao().findAll(hasOwner, pageable);
        } else {
            Set<Long> visibleIds = getSecurityService().getVisibleObjectIds(getEntityClass(), getDao().findAllIds());
            BooleanExpression isVisible = getIdPath().in(visibleIds);
            return getDao().findAll(hasOwner.and(isVisible), pageable);
        }
    }

    abstract NumberPath<Long> getIdPath();

    abstract QUser getOwnerPath();

    abstract Class<T> getEntityClass();

    abstract FtepSecurityService getSecurityService();

    abstract FtepEntityDao<T> getDao();

}
