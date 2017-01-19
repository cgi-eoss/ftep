package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.FtepJob;
import com.cgi.eoss.ftep.persistence.dao.FtepJobDao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "jobs", itemResourceRel = "job", collectionResourceRel = "jobs")
public interface JobsApi extends PagingAndSortingRepository<FtepJob, Long>, FtepJobDao {
}
