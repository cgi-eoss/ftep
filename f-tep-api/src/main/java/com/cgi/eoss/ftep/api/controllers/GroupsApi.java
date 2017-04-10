package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.projections.ShortGroup;
import com.cgi.eoss.ftep.model.Group;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(
        path = "groups",
        itemResourceRel = "group",
        collectionResourceRel = "groups",
        excerptProjection = ShortGroup.class)
public interface GroupsApi extends GroupsApiInferringOwner, CrudRepository<Group, Long> {

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    <S extends Group> Iterable<S> save(Iterable<S> groups);

    @Override
    @PreAuthorize("(#group.id == null) or hasRole('ROLE_ADMIN') or hasPermission(#group, 'administration')")
    <S extends Group> S save(@P("group") S group);

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void delete(Iterable<? extends Group> groups);

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#group, 'administration')")
    void delete(@P("group") Group group);

}
