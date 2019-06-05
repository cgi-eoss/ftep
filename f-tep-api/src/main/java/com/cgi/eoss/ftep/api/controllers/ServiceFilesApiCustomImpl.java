package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.QFtepServiceContextFile;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceContextFileDao;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.google.common.io.BaseEncoding;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ServiceFilesApiCustomImpl extends BaseRepositoryApiImpl<FtepServiceContextFile> implements ServiceFilesApiCustom {

    private final FtepSecurityService securityService;
    private final FtepServiceContextFileDao dao;

    public ServiceFilesApiCustomImpl(FtepSecurityService securityService, FtepServiceContextFileDao dao) {
        super(FtepServiceContextFile.class);
        this.securityService = securityService;
        this.dao = dao;
    }

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
