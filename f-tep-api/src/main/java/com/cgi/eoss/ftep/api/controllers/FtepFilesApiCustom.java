package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepFileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FtepFilesApiCustom {
    Page<FtepFile> findByType(FtepFileType type, Pageable pageable);
}
