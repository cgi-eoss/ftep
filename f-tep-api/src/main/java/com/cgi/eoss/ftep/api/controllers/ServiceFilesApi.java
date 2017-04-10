package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.projections.ShortFtepServiceContextFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RepositoryRestResource(
        path = "serviceFiles",
        itemResourceRel = "serviceFile",
        collectionResourceRel = "serviceFiles",
        excerptProjection = ShortFtepServiceContextFile.class
)
public interface ServiceFilesApi extends ServiceFilesApiCustom, CrudRepository<FtepServiceContextFile, Long> {

    @Override
    @RestResource(exported = false)
    List<FtepServiceContextFile> findAll();

    @Override
    @PostAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(returnObject, 'read')")
    FtepServiceContextFile findOne(Long id);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#serviceFile.service, 'write')")
    <S extends FtepServiceContextFile> S save(@P("serviceFile") S serviceFile);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    <S extends FtepServiceContextFile> Iterable<S> save(@P("serviceFiles") Iterable<S> serviceFiles);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#serviceFile.service, 'administration')")
    void delete(@P("serviceFile") FtepServiceContextFile service);

    @PostFilter("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(filterObject, 'read')")
    List<FtepServiceContextFile> findByService(@Param("service") FtepService service);

}
