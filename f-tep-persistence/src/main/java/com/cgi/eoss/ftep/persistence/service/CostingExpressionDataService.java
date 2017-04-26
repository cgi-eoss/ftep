package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.CostingExpression;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;

public interface CostingExpressionDataService extends
        FtepEntityDataService<CostingExpression> {
    CostingExpression getServiceCostingExpression(FtepService service);
    CostingExpression getDownloadCostingExpression(FtepFile ftepFile);
}
