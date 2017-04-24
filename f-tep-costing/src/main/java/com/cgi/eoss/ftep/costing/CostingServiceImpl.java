package com.cgi.eoss.ftep.costing;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Wallet;
import org.springframework.expression.ExpressionParser;

/**
 * <p>Default implementation of {@link CostingService}.</p>
 */
public class CostingServiceImpl implements CostingService {

    private final ExpressionParser expressionParser;

    public CostingServiceImpl(ExpressionParser costingExpressionParser) {
        this.expressionParser = costingExpressionParser;
    }

    @Override
    public Integer estimateJobCost(JobConfig jobConfig) {
        return null;
    }

    @Override
    public Integer estimateDownloadCost(FtepFile download) {
        return null;
    }

    @Override
    public void chargeForJob(Wallet wallet, Job job) {

    }

    @Override
    public void chargeForDownload(Wallet wallet, FtepFile download) {

    }

}
