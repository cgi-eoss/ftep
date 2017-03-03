package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.projections.ShortDatabasket;
import com.cgi.eoss.ftep.model.Databasket;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(
        path = "databaskets",
        itemResourceRel = "databasket",
        collectionResourceRel = "databaskets",
        excerptProjection = ShortDatabasket.class)
public interface DatabasketsApi extends DatabasketsApiInferringOwner, CrudRepository<Databasket, Long> {

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    <S extends Databasket> Iterable<S> save(Iterable<S> databaskets);

    @Override
    @PreAuthorize("(#databasket.id == null) or hasRole('ROLE_ADMIN') or hasPermission(#databasket, 'administration')")
    <S extends Databasket> S save(@P("databasket") S databasket);

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void delete(Iterable<? extends Databasket> databaskets);

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#databasket, 'administration')")
    void delete(@P("databasket") Databasket databasket);

}
