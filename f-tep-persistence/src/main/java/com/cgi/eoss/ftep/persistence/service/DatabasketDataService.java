package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepDatabasket;
import com.cgi.eoss.ftep.model.FtepUser;

import java.util.List;

public interface DatabasketDataService extends
        FtepEntityDataService<FtepDatabasket>,
        SearchableDataService<FtepDatabasket> {
    List<FtepDatabasket> findByOwner(FtepUser user);
}
