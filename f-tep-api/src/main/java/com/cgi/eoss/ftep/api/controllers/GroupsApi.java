package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(path = "groups", itemResourceRel = "group", collectionResourceRel = "groups", excerptProjection = ShortGroup.class)
public interface GroupsApi extends BaseRepositoryApi<Group>, GroupsApiCustom, PagingAndSortingRepository<Group, Long> {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends Group> Iterable<S> save(Iterable<S> groups);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#group.id == null) or hasPermission(#group, 'administration')")
    <S extends Group> S save(@P("group") S group);

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    Group findOne(Long id);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(Iterable<? extends Group> groups);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#group, 'administration')")
    void delete(@P("group") Group group);

    @Override
    @Query("select t from Group t where t.owner=user")
    Page<Group> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from Group t where not t.owner=user")
    Page<Group> findByNotOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path="findByFilterOnly", rel="findByFilterOnly")
    @Query("select t from Group t where t.name like %:filter% or t.description like %:filter%")
    Page<Group> findByFilterOnly(@Param("filter") String filter, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndOwner", rel = "findByFilterAndOwner")
    @Query("select t from Group t where t.owner=:owner and (t.name like %:filter% or t.description like %:filter%)")
    Page<Group> findByFilterAndOwner(@Param("filter") String filter, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndNotOwner", rel = "findByFilterAndNotOwner")
    @Query("select t from Group t where not t.owner=:owner and (t.name like %:filter% or t.description like %:filter%)")
    Page<Group> findByFilterAndNotOwner(@Param("filter") String filter, @Param("owner") User user, Pageable pageable);
}
