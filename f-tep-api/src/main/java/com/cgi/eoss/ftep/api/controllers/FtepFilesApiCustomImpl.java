package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.QFtepFile;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepFileDao;
import com.cgi.eoss.ftep.security.FtepSecurityService;
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

import java.io.IOException;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class FtepFilesApiCustomImpl extends BaseRepositoryApiImpl<FtepFile> implements FtepFilesApiCustom {

    private final FtepSecurityService securityService;
    private final FtepFileDao dao;
    private final CatalogueService catalogueService;

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
        return getFilteredResults(getFilterPredicate(null, type, null, null, null, null), pageable);
    }

    @Override
    public Page<FtepFile> searchByFilterOnly(String filter, FtepFile.Type type, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, type, null, null, null, null), pageable);
    }

    @Override
    public Page<FtepFile> searchByFilterAndOwner(String filter, FtepFile.Type type, User user, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, type, user, null, null, null), pageable);
    }

    @Override
    public Page<FtepFile> searchByFilterAndNotOwner(String filter, FtepFile.Type type, User user, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, type, null, user, null, null), pageable);
    }

    @Override
    public Page<FtepFile> searchAll(String keyword, FtepFile.Type type, User owner, User notOwner, Long minFilesize, Long maxFilesize, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(keyword, type, owner, notOwner, minFilesize, maxFilesize), pageable);
    }

    private Predicate getFilterPredicate(String filter, FtepFile.Type type, User owner, User notOwner, Long minFilesize, Long maxFilesize) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QFtepFile.ftepFile.filename.containsIgnoreCase(filter));
        }

        if (type != null) {
            builder.and(QFtepFile.ftepFile.type.eq(type));
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
