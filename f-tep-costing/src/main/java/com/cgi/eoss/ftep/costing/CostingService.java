package com.cgi.eoss.ftep.costing;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Wallet;

/**
 * <p>Service to expose F-TEP activity cost estimations and the charge mechanism.</p>
 */
public interface CostingService {

    public Integer estimateJobCost(JobConfig jobConfig);
    public Integer estimateSingleRunJobCost(JobConfig jobConfig);
    public Integer estimateDownloadCost(FtepFile download);

    public void chargeForJob(Wallet wallet, Job job);
    public void chargeForDownload(Wallet wallet, FtepFile download);
}
