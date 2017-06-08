package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServicesApiCustom {
    Page<FtepService> findByFilterOnly(String filter, FtepService.Type serviceType, Pageable pageable);

    Page<FtepService> findByFilterAndOwner(String filter, User user, FtepService.Type serviceType, Pageable pageable);

    Page<FtepService> findByFilterAndNotOwner(String filter, User user, FtepService.Type serviceType, Pageable pageable);
}
