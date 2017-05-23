package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(
        path = "projects",
        itemResourceRel = "project",
        collectionResourceRel = "projects",
        excerptProjection = ShortProject.class)
public interface ProjectsApi extends BaseRepositoryApi<Project>, ProjectsApiCustom, PagingAndSortingRepository<Project, Long> {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends Project> Iterable<S> save(Iterable<S> projects);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#project.id == null) or hasPermission(#project, 'write')")
    <S extends Project> S save(@P("project") S project);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(Iterable<? extends Project> projects);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#project, 'administration')")
    void delete(@P("project") Project project);

    @Override
    @Query("select t from Project t where t.owner=user")
    Page<Project> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from Project t where not t.owner=user")
    Page<Project> findByNotOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterOnly", rel = "findByFilterOnly")
    @Query("select t from Project t where t.name like CONCAT('%', filter, '%') or t.description like CONCAT('%', filter, '%')")
    Page<Project> findByFilterOnly(@Param("filter") String filter, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndOwner", rel = "findByFilterAndOwner")
    @Query("select t from Project t where t.owner=user and (t.name like CONCAT('%', filter, '%') or t.description like CONCAT('%', filter, '%'))")
    Page<Project> findByFilterAndOwner(@Param("filter") String filter, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndNotOwner", rel = "findByFilterAndNotOwner")
    @Query("select t from Project t where not t.owner=user and (t.name like CONCAT('%', filter, '%') or t.description like CONCAT('%', filter, '%'))")
    Page<Project> findByFilterAndNotOwner(@Param("filter") String filter, @Param("owner") User user, Pageable pageable);
}
