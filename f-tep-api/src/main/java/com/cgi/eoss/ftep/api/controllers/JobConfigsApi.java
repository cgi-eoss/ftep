package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(path = "jobConfigs", itemResourceRel = "jobConfig", collectionResourceRel = "jobConfigs")
public interface JobConfigsApi extends BaseRepositoryApi<JobConfig>, PagingAndSortingRepository<JobConfig, Long> {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends JobConfig> Iterable<S> save(Iterable<S> jobConfigs);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#jobConfig.id == null) or hasPermission(#jobConfig, 'write')")
    <S extends JobConfig> S save(@P("jobConfig") S jobConfig);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(Iterable<? extends JobConfig> groups);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'administration')")
    void delete(@P("jobConfig") JobConfig jobConfig);

    @Override
    @Query("select t from JobConfig t where t.owner=:owner")
    Page<JobConfig> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from JobConfig t where not t.owner=:owner")
    Page<JobConfig> findByNotOwner(@Param("owner") User user, Pageable pageable);

}
