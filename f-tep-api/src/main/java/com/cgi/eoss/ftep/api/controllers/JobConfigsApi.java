package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.JobConfig;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(path = "jobConfigs", itemResourceRel = "jobConfig", collectionResourceRel = "jobConfigs")
public interface JobConfigsApi extends PagingAndSortingRepository<JobConfig, Long>, JobConfigsApiCustom {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends JobConfig> Iterable<S> saveAll(Iterable<S> jobConfigs);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#jobConfig.id == null) or hasPermission(#jobConfig, 'write')")
    <S extends JobConfig> S save(@Param("jobConfig") S jobConfig);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void deleteAll(Iterable<? extends JobConfig> groups);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'administration')")
    void delete(@Param("jobConfig") JobConfig jobConfig);

}
