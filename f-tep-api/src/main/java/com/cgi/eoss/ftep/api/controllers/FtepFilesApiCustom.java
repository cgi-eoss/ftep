package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FtepFilesApiCustom {
    Page<FtepFile> findByType(FtepFile.Type type, Pageable pageable);
}
