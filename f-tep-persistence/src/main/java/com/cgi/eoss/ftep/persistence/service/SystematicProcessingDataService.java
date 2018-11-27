package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.SystematicProcessing;
import com.cgi.eoss.ftep.model.SystematicProcessing.Status;

import java.util.List;

public interface SystematicProcessingDataService extends FtepEntityDataService<SystematicProcessing> {
    public List<SystematicProcessing> findByStatus(Status s);
}
