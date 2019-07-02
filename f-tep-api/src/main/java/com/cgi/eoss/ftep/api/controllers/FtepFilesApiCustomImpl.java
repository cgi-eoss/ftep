package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.QFtepFile;
import com.cgi.eoss.ftep.model.QJob;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepFileDao;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.google.common.base.Strings;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPQLQuery;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Getter
@Component
public class FtepFilesApiCustomImpl extends BaseRepositoryApiImpl<FtepFile> implements FtepFilesApiCustom {

    private final FtepSecurityService securityService;
    private final FtepFileDao dao;
    private final CatalogueService catalogueService;

    public FtepFilesApiCustomImpl(FtepSecurityService securityService, FtepFileDao dao, CatalogueService catalogueService) {
        super(FtepFile.class);
        this.securityService = securityService;
        this.dao = dao;
        this.catalogueService = catalogueService;
    }

    @Override
    NumberPath<Long> getIdPath() {
        return QFtepFile.ftepFile.id;
    }

    @Override
    QUser getOwnerPath() {
        return QFtepFile.ftepFile.owner;
    }

    @Override
    Class<FtepFile> getEntityClass() {
        return FtepFile.class;
    }

    @Override
    public void delete(FtepFile ftepFile) {
        try {
            catalogueService.delete(ftepFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Page<FtepFile> searchByType(FtepFile.Type type, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(null, type, null, null, null, null, null), pageable);
    }

    @Override
    public Page<FtepFile> searchByFilterOnly(String filter, FtepFile.Type type, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, type, null, null, null, null, null), pageable);
    }

    @Override
    public Page<FtepFile> searchByFilterAndOwner(String filter, FtepFile.Type type, User user, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, type, null, user, null, null, null), pageable);
    }

    @Override
    public Page<FtepFile> searchByFilterAndNotOwner(String filter, FtepFile.Type type, User user, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, type, null, null, user, null, null), pageable);
    }

    @Override
    public Page<FtepFile> searchAll(String keyword, FtepFile.Type type, FtepFile.Type notType, User owner, User notOwner, Long minFilesize, Long maxFilesize, Pageable pageable) {
        Predicate predicate = getFilterPredicate(keyword, type, notType, owner, notOwner, minFilesize, maxFilesize);

        JPQLQuery<FtepFile> query = from(QFtepFile.ftepFile).where(predicate);

        query = getQuerydsl().applyPagination(pageable, query);

        if (getSecurityService().isSuperUser()) {
            return PageableExecutionUtils.getPage(query.fetch(), pageable, query::fetchCount);
        } else {
            Set<Long> visibleIds = getSecurityService().getVisibleObjectIds(getEntityClass(), getDao().findAllIds());
            query.where(getIdPath().in(visibleIds));
            return PageableExecutionUtils.getPage(query.fetch(), pageable, query::fetchCount);
        }
    }

    private Predicate getFilterPredicate(String filter, FtepFile.Type type, FtepFile.Type notType, User owner, User notOwner, Long minFilesize, Long maxFilesize) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QFtepFile.ftepFile.filename.containsIgnoreCase(filter)
                    .or(QFtepFile.ftepFile.job.any().config.label.containsIgnoreCase(filter))
            );
        }

        if (type != null) {
            builder.and(QFtepFile.ftepFile.type.eq(type));
        }

        if (notType != null) {
            builder.and(QFtepFile.ftepFile.type.ne(notType));
        }

        if (owner != null) {
            builder.and(QFtepFile.ftepFile.owner.eq(owner));
        }

        if (notOwner != null) {
            builder.and(QFtepFile.ftepFile.owner.ne(notOwner));
        }

        if (minFilesize != null) {
            builder.and(QFtepFile.ftepFile.filesize.goe(minFilesize));
        }

        if (maxFilesize != null) {
            builder.and(QFtepFile.ftepFile.filesize.loe(maxFilesize));
        }

        return builder.getValue();
    }

}
