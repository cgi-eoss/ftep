package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.persistence.dao.JobConfigDao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "jobConfigs", itemResourceRel = "jobConfig", collectionResourceRel = "jobConfigs")
public interface JobConfigsApi extends PagingAndSortingRepository<JobConfig, Long>, JobConfigDao {
}
