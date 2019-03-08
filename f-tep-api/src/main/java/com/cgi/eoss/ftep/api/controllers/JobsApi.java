package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.Job.Status;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Collection;
import java.util.Optional;

@RepositoryRestResource(path = "jobs", itemResourceRel = "job", collectionResourceRel = "jobs", excerptProjection = ShortJob.class)
public interface JobsApi extends JobsApiCustom, PagingAndSortingRepository<Job, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject.get(), 'read')")
    Optional<Job> findById(Long id);

    // Users cannot create job instances via the API; they are set via the service launch ingest
    @Override
    @RestResource(exported = false)
    <S extends Job> Iterable<S> saveAll(Iterable<S> jobs);

    // Users cannot create job instances via the API; they are set via the service launch ingest
    @Override
    @RestResource(exported = false)
    <S extends Job> S save(S job);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void deleteAll(Iterable<? extends Job> jobs);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'administration')")
    void delete(@Param("job") Job job);

    @Override
    @RestResource(path = "findByFilterOnly", rel = "findByFilterOnly")
    Page<Job> searchByFilterOnly(@Param("filter") String filter, @Param("status") Collection<Status> statuses, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndOwner", rel = "findByFilterAndOwner")
    Page<Job> searchByFilterAndOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndNotOwner", rel = "findByFilterAndNotOwner")
    Page<Job> searchByFilterAndNotOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndIsNotSubjob", rel = "findByFilterAndIsNotSubjob")
    Page<Job> searchByFilterAndIsNotSubjob(@Param("filter") String filter, @Param("status") Collection<Status> statuses, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndIsNotSubjobAndOwner", rel = "findByFilterAndIsNotSubjobAndOwner")
    Page<Job> searchByFilterAndIsNotSubjobAndOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndIsNotSubjobAndNotOwner", rel = "findByFilterAndIsNotSubjobAndNotOwner")
    Page<Job> searchByFilterAndIsNotSubjobAndNotOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndParent", rel = "findByFilterAndParent")
    Page<Job> searchByFilterAndParent(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("parentId") Long parentId, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndParentAndOwner", rel = "findByFilterAndParentAndOwner")
    Page<Job> searchByFilterAndParentAndOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("parentId") Long parentId, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndParentAndNotOwner", rel = "findByFilterAndParentAndNotOwner")
    Page<Job> searchByFilterAndParentAndNotOwner(@Param("filter") String filter, @Param("status") Collection<Status> statuses, @Param("parentId") Long parentId, @Param("owner") User user, Pageable pageable);
}
