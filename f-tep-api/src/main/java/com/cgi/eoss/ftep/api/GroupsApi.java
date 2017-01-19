package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.FtepGroup;
import com.cgi.eoss.ftep.persistence.dao.FtepGroupDao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "groups", itemResourceRel = "group", collectionResourceRel = "groups")
public interface GroupsApi extends PagingAndSortingRepository<FtepGroup, Long>, FtepGroupDao {
}
