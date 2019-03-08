package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.CostingExpression;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;

import java.util.Optional;

public interface CostingExpressionDataService extends
        FtepEntityDataService<CostingExpression> {
    Optional<CostingExpression> getServiceCostingExpression(FtepService service);
    Optional<CostingExpression> getDownloadCostingExpression(FtepFile ftepFile);
}
