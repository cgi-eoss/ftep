package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.PublishingRequest;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortPublishingRequest;
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

@RepositoryRestResource(path = "publishingRequests", itemResourceRel = "publishingRequest", collectionResourceRel = "publishingRequests", excerptProjection = ShortPublishingRequest.class)
public interface PublishingRequestsApi extends BaseRepositoryApi<PublishingRequest>, PublishingRequestsApiCustom, PagingAndSortingRepository<PublishingRequest, Long> {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends PublishingRequest> Iterable<S> save(Iterable<S> publishingRequests);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends PublishingRequest> S save(@P("publishingRequest") S publishingRequest);

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or authentication.name.equals(returnObject.owner.name)")
    PublishingRequest findOne(Long id);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(Iterable<? extends PublishingRequest> publishingRequests);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(@P("publishingRequest") PublishingRequest publishingRequest);

    @Override
    @Query("select t from PublishingRequest t where t.owner=user")
    Page<PublishingRequest> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from PublishingRequest t where not t.owner=user")
    Page<PublishingRequest> findByNotOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByStatus")
    @Query("select t from PublishingRequest t where t.status in (statuses)")
    Page<PublishingRequest> findByStatus(@Param("status") Collection<PublishingRequest.Status> statuses, Pageable pageable);

}
