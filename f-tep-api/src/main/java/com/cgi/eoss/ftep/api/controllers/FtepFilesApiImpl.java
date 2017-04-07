package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepFileType;
import com.cgi.eoss.ftep.model.QFtepFile;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.persistence.dao.FtepFileDao;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.Set;

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
    public Page<FtepFile> findByType(@Param("type") FtepFileType type, Pageable pageable) {
        BooleanExpression isType = QFtepFile.ftepFile.type.eq(type);

        if (getSecurityService().isSuperUser()) {
            return getDao().findAll(isType, pageable);
        } else {
            Set<Long> visibleIds = getSecurityService().getVisibleObjectIds(getEntityClass(), getDao().findAllIds());
            BooleanExpression isVisible = QFtepFile.ftepFile.id.in(visibleIds);
            return getDao().findAll(isType.and(isVisible), pageable);
        }
    }

}
