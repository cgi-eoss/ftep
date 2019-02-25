package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(
        path = "projects",
        itemResourceRel = "project",
        collectionResourceRel = "projects",
        excerptProjection = ShortProject.class)
public interface ProjectsApi extends ProjectsApiCustom, PagingAndSortingRepository<Project, Long> {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends Project> Iterable<S> saveAll(Iterable<S> projects);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#project.id == null) or hasPermission(#project, 'write')")
    <S extends Project> S save(@Param("project") S project);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void deleteAll(Iterable<? extends Project> projects);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#project, 'administration')")
    void delete(@Param("project") Project project);

    @Override
    @RestResource(path = "findByFilterOnly", rel = "findByFilterOnly")
    Page<Project> searchByFilterOnly(@Param("filter") String filter, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndOwner", rel = "findByFilterAndOwner")
    Page<Project> searchByFilterAndOwner(@Param("filter") String filter, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndNotOwner", rel = "findByFilterAndNotOwner")
    Page<Project> searchByFilterAndNotOwner(@Param("filter") String filter, @Param("owner") User user, Pageable pageable);
}
