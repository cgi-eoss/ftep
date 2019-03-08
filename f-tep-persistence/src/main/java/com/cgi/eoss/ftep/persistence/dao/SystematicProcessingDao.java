package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.SystematicProcessing;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface SystematicProcessingDao extends FtepEntityDao<SystematicProcessing> {
    public List<SystematicProcessing> findByOwner(User user);
}
