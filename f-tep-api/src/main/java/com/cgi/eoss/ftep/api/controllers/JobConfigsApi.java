package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.JobConfig;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(path = "jobConfigs", itemResourceRel = "jobConfig", collectionResourceRel = "jobConfigs")
public interface JobConfigsApi extends JobConfigsApiInferringOwner, CrudRepository<JobConfig, Long> {

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    <S extends JobConfig> Iterable<S> save(Iterable<S> jobConfigs);

    @Override
    @PreAuthorize("(#jobConfig.id == null) or hasRole('ROLE_ADMIN') or hasPermission(#jobConfig, 'write')")
    <S extends JobConfig> S save(@P("jobConfig") S jobConfig);

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void delete(Iterable<? extends JobConfig> groups);

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#jobConfig, 'administration')")
    void delete(@P("jobConfig") JobConfig jobConfig);

}
