package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.JobConfig;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "jobConfigs", itemResourceRel = "jobConfig", collectionResourceRel = "jobConfigs")
public interface JobConfigsApi extends CrudRepository<JobConfig, Long> {
}
