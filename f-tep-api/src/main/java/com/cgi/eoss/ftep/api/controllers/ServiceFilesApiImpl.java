package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.QFtepServiceContextFile;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceContextFileDao;
import com.google.common.io.BaseEncoding;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class ServiceFilesApiImpl extends BaseRepositoryApiImpl<FtepServiceContextFile> implements ServiceFilesApiCustom {

    private final FtepSecurityService securityService;
    private final FtepServiceContextFileDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QFtepServiceContextFile.ftepServiceContextFile.id;
    }

    @Override
    QUser getOwnerPath() {
        return QFtepServiceContextFile.ftepServiceContextFile.service.owner;
    }

    @Override
    Class<FtepServiceContextFile> getEntityClass() {
        return FtepServiceContextFile.class;
    }

    @Override
    public <S extends FtepServiceContextFile> S save(S serviceFile) {
        // Transform base64 content into real content
        serviceFile.setContent(new String(BaseEncoding.base64().decode(serviceFile.getContent())));
        return getDao().save(serviceFile);
    }

}
