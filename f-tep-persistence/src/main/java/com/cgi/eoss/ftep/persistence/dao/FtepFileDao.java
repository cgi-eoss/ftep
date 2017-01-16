package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepFile;

import java.util.List;

public interface FtepFileDao extends FtepEntityDao<FtepFile> {
    List<FtepFile> findByNameContainingIgnoreCase(String term);
}
