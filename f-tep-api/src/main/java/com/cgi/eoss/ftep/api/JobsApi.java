package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.Job;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "jobs", itemResourceRel = "job", collectionResourceRel = "jobs")
public interface JobsApi extends CrudRepository<Job, Long> {
}
