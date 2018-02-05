package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FtepFilesApiCustom {
    void delete(FtepFile ftepFile);

    Page<FtepFile> findByType(FtepFile.Type type, Pageable pageable);

    Page<FtepFile> findByFilterOnly(String filter, FtepFile.Type type, Pageable pageable);

    Page<FtepFile> findByFilterAndOwner(String filter, FtepFile.Type type, User user, Pageable pageable);

    Page<FtepFile> findByFilterAndNotOwner(String filter, FtepFile.Type type, User user, Pageable pageable);
}
