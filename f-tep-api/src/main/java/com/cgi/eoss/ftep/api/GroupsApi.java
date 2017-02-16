package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.Group;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "groups", itemResourceRel = "group", collectionResourceRel = "groups")
public interface GroupsApi extends CrudRepository<Group, Long> {
}
