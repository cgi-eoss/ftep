package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepDatabasket;
import com.cgi.eoss.ftep.model.FtepUser;

import java.util.List;

public interface FtepDatabasketDao extends FtepEntityDao<FtepDatabasket> {
    List<FtepDatabasket> findByNameContainingIgnoreCase(String term);

    List<FtepDatabasket> findByOwner(FtepUser user);
}
