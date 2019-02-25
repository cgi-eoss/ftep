package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServicesApiCustom extends BaseRepositoryApi<FtepService> {
    Page<FtepService> searchByFilterOnly(String filter, FtepService.Type serviceType, Pageable pageable);

    Page<FtepService> searchByFilterAndOwner(String filter, User user, FtepService.Type serviceType, Pageable pageable);

    Page<FtepService> searchByFilterAndNotOwner(String filter, User user, FtepService.Type serviceType, Pageable pageable);
}
