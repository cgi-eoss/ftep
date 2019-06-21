package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.SystematicProcessing;
import com.cgi.eoss.ftep.model.projections.ShortSystematicProcessing;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;

import java.util.Optional;

@RepositoryRestResource(path = "systematicProcessings", itemResourceRel = "systematicProcessing", collectionResourceRel = "systematicProcessings", excerptProjection = ShortSystematicProcessing.class)
public interface SystematicProcessingsApi extends SystematicProcessingsApiCustom, PagingAndSortingRepository<SystematicProcessing, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject.get(), 'read')")
    Optional<SystematicProcessing> findById(Long id);

    // Users cannot create systematic processing instances via the API; they are set via the service launch ingest
    @Override
    @RestResource(exported = false)
    <S extends SystematicProcessing> Iterable<S> saveAll(Iterable<S> systematicProcessings);

    // Users cannot create systematic processing instances via the API; they are set via the service launch ingest
    @Override
    @RestResource(exported = false)
    <S extends SystematicProcessing> S save(S systematicProcessings);

}
