package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortFtepServiceContextFile;
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

import java.util.List;

@RepositoryRestResource(
        path = "serviceFiles",
        itemResourceRel = "serviceFile",
        collectionResourceRel = "serviceFiles",
        excerptProjection = ShortFtepServiceContextFile.class
)
public interface ServiceFilesApi extends BaseRepositoryApi<FtepServiceContextFile>, ServiceFilesApiCustom, PagingAndSortingRepository<FtepServiceContextFile, Long> {

    @Override
    @RestResource(exported = false)
    List<FtepServiceContextFile> findAll();

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject.service, 'read')")
    FtepServiceContextFile findOne(Long id);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceFile.service, 'write')")
    <S extends FtepServiceContextFile> S save(@P("serviceFile") S serviceFile);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends FtepServiceContextFile> Iterable<S> save(@P("serviceFiles") Iterable<S> serviceFiles);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceFile.service, 'administration')")
    void delete(@P("serviceFile") FtepServiceContextFile service);

    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'read')")
    Page<FtepServiceContextFile> findByService(@Param("service") FtepService service, Pageable pageable);

    @Override
    @Query("select t from FtepServiceContextFile t where t.service.owner=:owner")
    Page<FtepServiceContextFile> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from FtepServiceContextFile t where not t.service.owner=:owner")
    Page<FtepServiceContextFile> findByNotOwner(@Param("owner") User user, Pageable pageable);

}
