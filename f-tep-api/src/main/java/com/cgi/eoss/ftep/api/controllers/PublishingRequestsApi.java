package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.PublishingRequest;
import com.cgi.eoss.ftep.model.projections.ShortPublishingRequest;
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

@RepositoryRestResource(path = "publishingRequests", itemResourceRel = "publishingRequest", collectionResourceRel = "publishingRequests", excerptProjection = ShortPublishingRequest.class)
public interface PublishingRequestsApi extends PublishingRequestsApiCustom, PagingAndSortingRepository<PublishingRequest, Long> {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends PublishingRequest> Iterable<S> saveAll(Iterable<S> publishingRequests);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends PublishingRequest> S save(@Param("publishingRequest") S publishingRequest);

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or authentication.name.equals(returnObject.get().owner.name)")
    Optional<PublishingRequest> findById(Long id);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void deleteAll(Iterable<? extends PublishingRequest> publishingRequests);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(@Param("publishingRequest") PublishingRequest publishingRequest);

    @Override
    @RestResource(path = "findByStatus", rel = "findByStatus")
    Page<PublishingRequest> searchByStatus(@Param("status") Collection<PublishingRequest.Status> statuses, Pageable pageable);

}
