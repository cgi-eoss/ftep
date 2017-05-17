package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface FtepFilesApiCustom {
    Page<FtepFile> findByType(FtepFile.Type type, Pageable pageable);

    Page<FtepFile> findByNameContainsIgnoreCaseAndType(@Param("filter") String filter,
            @Param("type") FtepFile.Type type, Pageable pageable);

    Page<FtepFile> findByNameContainsIgnoreCaseAndTypeAndOwner(@Param("filter") String filter,
            @Param("type") FtepFile.Type type, @Param("owner") User user, Pageable pageable);

    Page<FtepFile> findByNameContainsIgnoreCaseAndTypeAndNotOwner(@Param("filter") String filter,
            @Param("type") FtepFile.Type type, @Param("owner") User user, Pageable pageable);
}
