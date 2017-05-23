package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.QFtepFile;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepFileDao;
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
public class FtepFilesApiImpl extends BaseRepositoryApiImpl<FtepFile> implements FtepFilesApiCustom {

    private final FtepSecurityService securityService;
    private final FtepFileDao dao;

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
    public Page<FtepFile> findByType(FtepFile.Type type, Pageable pageable) {
        return getFilteredResults(QFtepFile.ftepFile.type.eq(type), pageable);
    }

    @Override
    public Page<FtepFile> findByFilterOnly(String filter, FtepFile.Type type, Pageable pageable) {
        return getFilteredResults(
                QFtepFile.ftepFile.filename.containsIgnoreCase(filter).and(QFtepFile.ftepFile.type.eq(type)), pageable);
    }

    @Override
    public Page<FtepFile> findByFilterAndOwner(String filter, FtepFile.Type type, User user, Pageable pageable) {
        if (Strings.isNullOrEmpty(filter)) {
            return getFilteredResults(getOwnerPath().eq(user).and(QFtepFile.ftepFile.type.eq(type)), pageable);
        } else {
            return getFilteredResults(getOwnerPath().eq(user)
                            .and(QFtepFile.ftepFile.filename.containsIgnoreCase(filter)).and(QFtepFile.ftepFile.type.eq(type)),
                    pageable);
        }
    }

    @Override
    public Page<FtepFile> findByFilterAndNotOwner(String filter, FtepFile.Type type, User user, Pageable pageable) {
        if (Strings.isNullOrEmpty(filter)) {
            return getFilteredResults(getOwnerPath().ne(user).and(QFtepFile.ftepFile.type.eq(type)), pageable);
        } else {
            return getFilteredResults(getOwnerPath().ne(user)
                            .and(QFtepFile.ftepFile.filename.containsIgnoreCase(filter)).and(QFtepFile.ftepFile.type.eq(type)),
                    pageable);
        }
    }

}
