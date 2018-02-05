package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;

import java.util.List;

public interface FtepServiceContextFileDao extends FtepEntityDao<FtepServiceContextFile> {
    List<FtepServiceContextFile> findByService(FtepService service);
}
