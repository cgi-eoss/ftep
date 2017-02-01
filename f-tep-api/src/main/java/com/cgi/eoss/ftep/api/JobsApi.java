package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.persistence.dao.JobDao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "jobs", itemResourceRel = "job", collectionResourceRel = "jobs")
public interface JobsApi extends PagingAndSortingRepository<Job, Long>, JobDao {
}
