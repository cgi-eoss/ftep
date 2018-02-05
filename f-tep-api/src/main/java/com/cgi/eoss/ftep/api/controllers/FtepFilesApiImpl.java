package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.QFtepFile;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepFileDao;
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
public class FtepFilesApiImpl extends BaseRepositoryApiImpl<FtepFile> implements FtepFilesApiCustom {

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
    public Page<FtepFile> findByType(FtepFile.Type type, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(null, type), pageable);
    }

    @Override
    public Page<FtepFile> findByFilterOnly(String filter, FtepFile.Type type, Pageable pageable) {
        return getFilteredResults(getFilterPredicate(filter, type), pageable);
    }

    @Override
    public Page<FtepFile> findByFilterAndOwner(String filter, FtepFile.Type type, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().eq(user).and(getFilterPredicate(filter, type)).and(QFtepFile.ftepFile.type.eq(type)),
                pageable);
    }

    @Override
    public Page<FtepFile> findByFilterAndNotOwner(String filter, FtepFile.Type type, User user, Pageable pageable) {
        return getFilteredResults(getOwnerPath().ne(user)
                        .and(getFilterPredicate(filter, type)).and(QFtepFile.ftepFile.type.eq(type)),
                pageable);
    }

    private Predicate getFilterPredicate(String filter, FtepFile.Type type) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isNullOrEmpty(filter)) {
            builder.and(QFtepFile.ftepFile.filename.containsIgnoreCase(filter));
        }

        if (type != null) {
            builder.and(QFtepFile.ftepFile.type.eq(type));
        }

        return builder.getValue();
    }

}
