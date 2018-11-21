package com.cgi.eoss.ftep.costing;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Wallet;

/**
 * <p>Service to expose F-TEP activity cost estimations and the charge mechanism.</p>
 */
public interface CostingService {

    Integer estimateJobCost(JobConfig jobConfig);
    Integer estimateSingleRunJobCost(JobConfig jobConfig);
    Integer estimateDownloadCost(FtepFile download);

    void chargeForJob(Wallet wallet, Job job);
    void chargeForDownload(Wallet wallet, FtepFile download);
}
