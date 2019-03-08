package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepServiceContextFile;

public interface ServiceFilesApiCustom extends BaseRepositoryApi<FtepServiceContextFile> {
    <S extends FtepServiceContextFile> S save(S service);
}
