package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.WorkerLocatorExpression;

public interface WorkerLocatorExpressionDao extends FtepEntityDao<WorkerLocatorExpression> {
    WorkerLocatorExpression findOneByService(FtepService service);
}
