package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceContextFileDao;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.google.common.io.BaseEncoding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Transactional
@Component
public class ServiceFilesApiImpl implements ServiceFilesApiCustom {

    private final FtepServiceContextFileDao contextFileDao;
    private final ServiceDataService serviceDataService;

    @Autowired
    public ServiceFilesApiImpl(FtepServiceContextFileDao contextFileDao, ServiceDataService serviceDataService) {
        this.contextFileDao = contextFileDao;
        this.serviceDataService = serviceDataService;
    }

    @Override
    public <S extends FtepServiceContextFile> S save(S serviceFile) {
        // Transform base64 content into real content
        serviceFile.setContent(new String(BaseEncoding.base64().decode(serviceFile.getContent())));
        return contextFileDao.save(serviceFile);
    }
}
