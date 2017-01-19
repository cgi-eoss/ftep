package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.FtepProject;
import com.cgi.eoss.ftep.persistence.dao.FtepProjectDao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "projects", itemResourceRel = "project", collectionResourceRel = "projects")
public interface ProjectsApi extends PagingAndSortingRepository<FtepProject, Long>, FtepProjectDao {
}
