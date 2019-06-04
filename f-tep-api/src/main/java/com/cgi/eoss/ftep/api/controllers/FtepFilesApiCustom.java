package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FtepFilesApiCustom extends BaseRepositoryApi<FtepFile> {
    void delete(FtepFile ftepFile);

    Page<FtepFile> searchByType(FtepFile.Type type, Pageable pageable);

    Page<FtepFile> searchByFilterOnly(String filter, FtepFile.Type type, Pageable pageable);

    Page<FtepFile> searchByFilterAndOwner(String filter, FtepFile.Type type, User user, Pageable pageable);

    Page<FtepFile> searchByFilterAndNotOwner(String filter, FtepFile.Type type, User user, Pageable pageable);

    Page<FtepFile> searchAll(String keyword, FtepFile.Type type, FtepFile.Type notType, User owner, User notOwner, Long minFilesize, Long maxFilesize, Pageable pageable);
}
