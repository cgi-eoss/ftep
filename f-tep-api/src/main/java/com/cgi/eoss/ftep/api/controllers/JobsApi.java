package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortJob;

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

import java.util.Collection;

@RepositoryRestResource(path = "jobs", itemResourceRel = "job", collectionResourceRel = "jobs", excerptProjection = ShortJob.class)
public interface JobsApi extends BaseRepositoryApi<Job>, JobsApiCustom, PagingAndSortingRepository<Job, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject, 'read')")
    Job findOne(Long id);

    // Users cannot create job instances via the API; they are set via the service launch ingest
    @Override
    @RestResource(exported = false)
    <S extends Job> Iterable<S> save(Iterable<S> jobs);

    // Users cannot create job instances via the API; they are set via the service launch ingest
    @Override
    @RestResource(exported = false)
    <S extends Job> S save(S job);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(Iterable<? extends Job> jobs);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'administration')")
    void delete(@P("job") Job job);

    @Override
    @Query("select t from Job t where t.owner=user")
    Page<Job> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from Job t where not t.owner=user")
    Page<Job> findByNotOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterOnly", rel = "findByFilterOnly")
    @Query("select t from Job t where (t.id like %:filter% or t.config.label like %:filter% or t.config.service.name like %:filter%) and t.status in (:status)")
    Page<Job> findByFilterOnly(@Param("filter") String filter, @Param("status") Collection<Status> statuses, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndOwner", rel = "findByFilterAndOwner")
    @Query("select t from Job t where (t.id like %:filter% or t.config.label like %:filter% or t.config.service.name like %:filter%) and t.status in (:status) and t.owner=:owner")
    Page<Job> findByFilterAndOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndNotOwner", rel = "findByFilterAndNotOwner")
    @Query("select t from Job t where (t.id like %:filter% or t.config.label like %:filter% or t.config.service.name like %:filter%) and t.status in (:status) and not t.owner=:owner")
    Page<Job> findByFilterAndNotOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndIsNotSubjob", rel = "findByFilterAndIsNotSubjob")
    @Query("select t from Job t where (t.id like %:filter% or t.config.label like %:filter% or t.config.service.name like %:filter%) and t.status in (:status) and t.parentJob=null")
    Page<Job> findByFilterAndIsNotSubjob(@Param("filter") String filter, @Param("status") Collection<Status> statuses, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndIsNotSubjobAndOwner", rel = "findByFilterAndIsNotSubjobAndOwner")
    @Query("select t from Job t where (t.id like %:filter% or t.config.label like %:filter% or t.config.service.name like %:filter%) and t.parentJob=null and t.status in (:status) and t.owner=:owner")
    Page<Job> findByFilterAndIsNotSubjobAndOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndIsNotSubjobAndNotOwner", rel = "findByFilterAndIsNotSubjobAndNotOwner")
    @Query("select t from Job t where (t.id like %:filter% or t.config.label like %:filter% or t.config.service.name like %:filter%) and t.parentJob=null and t.status in (:status) and not t.owner=:owner")
    Page<Job> findByFilterAndIsNotSubjobAndNotOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndParent", rel = "findByFilterAndParent")
    @Query("select t from Job t where (t.id like %:filter% or t.config.label like %:filter% or t.config.service.name like %:filter%) and t.parentJob.id=:parentId and t.status in (:status)")
    Page<Job> findByFilterAndParent(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("parentId") Long parentId, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndParentAndOwner", rel = "findByFilterAndParentAndOwner")
    @Query("select t from Job t where (t.id like %:filter% or t.config.label like %:filter% or t.config.service.name like %:filter%) and t.parentJob.id=:parentId and t.status in (:status) and t.owner=:owner")
    Page<Job> findByFilterAndParentAndOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("parentId") Long parentId, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndParentAndNotOwner", rel = "findByFilterAndParentAndNotOwner")
    @Query("select t from Job t where (t.id like %:filter% or t.config.label like %:filter% or t.config.service.name like %:filter%) and t.parentJob.id=:parentId and t.status in (:status) and not t.owner=:owner")
    Page<Job> findByFilterAndParentAndNotOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("parentId") Long parentId, @Param("owner") User user, Pageable pageable);
}
