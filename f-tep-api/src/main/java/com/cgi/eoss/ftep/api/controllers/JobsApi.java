package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(path = "jobs", itemResourceRel = "job", collectionResourceRel = "jobs")
public interface JobsApi extends CrudRepository<Job, Long> {

    // Users cannot create job instances via the API; they are set via the service launch process
    @Override
    @RestResource(exported = false)
    <S extends Job> Iterable<S> save(Iterable<S> jobs);

    // Users cannot create job instances via the API; they are set via the service launch process
    @Override
    @RestResource(exported = false)
    <S extends Job> S save(S job);

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void delete(Iterable<? extends Job> jobs);

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#job, 'administration')")
    void delete(@P("job") Job job);

}
