package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortFtepService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@RepositoryRestResource(path = "services", itemResourceRel = "service", collectionResourceRel = "services", excerptProjection = ShortFtepService.class)
public interface ServicesApi extends ServicesApiCustom, JpaRepository<FtepService, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (returnObject.get().status == T(com.cgi.eoss.ftep.model.FtepService$Status).AVAILABLE) or hasPermission(returnObject.get(), 'read')")
    Optional<FtepService> findById(Long id);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#service.id == null and hasRole('EXPERT_USER')) or (#service.id != null && hasPermission(#service, 'write'))")
    <S extends FtepService> S save(@Param("service") S service);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void deleteAll(Iterable<? extends FtepService> services);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (!@ftepSecurityService.isPublic(#service.class, #service.id) and hasPermission(#service, 'administration'))")
    void delete(@Param("service") FtepService service);

    @Override
    @RestResource(path = "findByFilterOnly", rel = "findByFilterOnly")
    Page<FtepService> searchByFilterOnly(@Param("filter") String filter, @Param("serviceType") @RequestParam(required = false) FtepService.Type serviceType, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndOwner", rel = "findByFilterAndOwner")
    Page<FtepService> searchByFilterAndOwner(@Param("filter") String filter, @Param("owner") User user, @Param("serviceType") @RequestParam(required = false) FtepService.Type serviceType, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndNotOwner", rel = "findByFilterAndNotOwner")
    Page<FtepService> searchByFilterAndNotOwner(@Param("filter") String filter, @Param("owner") User user, @Param("serviceType") @RequestParam(required = false) FtepService.Type serviceType, Pageable pageable);
}
