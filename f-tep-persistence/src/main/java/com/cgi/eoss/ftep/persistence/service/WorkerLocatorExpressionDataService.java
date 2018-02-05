package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.WorkerLocatorExpression;

public interface WorkerLocatorExpressionDataService extends
        FtepEntityDataService<WorkerLocatorExpression> {
    WorkerLocatorExpression getByService(FtepService service);
}
