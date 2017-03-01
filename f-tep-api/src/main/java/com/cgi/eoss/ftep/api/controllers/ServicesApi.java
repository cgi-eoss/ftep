package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.projections.ShortFtepService;
import com.cgi.eoss.ftep.model.FtepService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RepositoryRestResource(
        path = "services",
        itemResourceRel = "service",
        collectionResourceRel = "services",
        excerptProjection = ShortFtepService.class
)
public interface ServicesApi extends ServicesApiInferringOwner, CrudRepository<FtepService, Long> {

    // TODO Evaluate performance and prefer limiting by query rather than @PostFilter
    @Override
    @PostFilter("filterObject.status == T(com.cgi.eoss.ftep.model.ServiceStatus).AVAILABLE or hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(filterObject, 'read')")
    List<FtepService> findAll();

    @Override
    @PostAuthorize("returnObject.status == T(com.cgi.eoss.ftep.model.ServiceStatus).AVAILABLE or hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(returnObject, 'read')")
    FtepService findOne(Long id);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or (#service.id != null and hasPermission(#service, 'write'))")
    <S extends FtepService> S save(@P("service") S service);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    void delete(Iterable<? extends FtepService> services);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    void delete(FtepService service);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    void delete(Long id);

}
