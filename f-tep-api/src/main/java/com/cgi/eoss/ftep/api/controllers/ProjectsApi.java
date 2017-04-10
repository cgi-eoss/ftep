package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.projections.ShortProject;
import com.cgi.eoss.ftep.model.Project;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(
        path = "projects",
        itemResourceRel = "project",
        collectionResourceRel = "projects",
        excerptProjection = ShortProject.class)
public interface ProjectsApi extends ProjectsApiInferringOwner, CrudRepository<Project, Long> {

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    <S extends Project> Iterable<S> save(Iterable<S> projects);

    @Override
    @PreAuthorize("(#project.id == null) or hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#project, 'write')")
    <S extends Project> S save(@P("project") S project);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    void delete(Iterable<? extends Project> projects);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#project, 'administration')")
    void delete(@P("project") Project project);

}
