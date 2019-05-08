package com.cgi.eoss.ftep.batch.service;

import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;

import java.util.List;

/**
 * <p>Service to evaluate and expand JobParam parameters. This includes:</p>
 * <ol>
 * <li>Evaluate JobParams with searchParameter=true and replace the values with the search results</li>
 * <li>Evaluate JobParams and replace databaskets with their contents</li>
 * <li>Evaluate JobParams with parallelParameter=true and expand to one JobSpec per value</li>
 * </ol>
 */
public interface JobExpansionService {

    /**
     * <p>Expand a service launching request into its decomposed job specifications.</p>
     * @param request
     * @return
     */
    List<JobSpec> expandJobParamsFromRequest(FtepServiceParams request);

    /**
     * <p>Evaluate the job parameters of a job configuration and expand the results into sub-job configurations.</p>
     * @param jobConfig
     * @param alreadyExpanded
     * @return
     */
    List<JobConfig> expandJobParamsFromJobConfig(JobConfig jobConfig, boolean alreadyExpanded);

}
