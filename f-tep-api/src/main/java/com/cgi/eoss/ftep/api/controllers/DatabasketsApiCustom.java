package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DatabasketsApiCustom extends BaseRepositoryApi<Databasket> {
    Page<Databasket> searchByFilterOnly(String filter, Pageable pageable);

    Page<Databasket> searchByFilterAndOwner(String filter, User user, Pageable pageable);

    Page<Databasket> searchByFilterAndNotOwner(String filter, User user, Pageable pageable);
}
