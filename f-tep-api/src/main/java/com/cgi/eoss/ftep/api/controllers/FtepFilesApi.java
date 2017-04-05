package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.projections.ShortFtepFile;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepFileType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;
import java.util.List;

@RepositoryRestResource(
        path = "ftepFiles",
        itemResourceRel = "ftepFile",
        collectionResourceRel = "ftepFiles",
        excerptProjection = ShortFtepFile.class)
public interface FtepFilesApi extends CrudRepository<FtepFile, Long> {

    // TODO Evaluate performance and prefer limiting by query rather than @PostFilter
    @Override
    @PostFilter("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(filterObject, 'read')")
    List<FtepFile> findAll();

    @Override
    @PostAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(returnObject, 'read')")
    FtepFile findOne(Long id);

    @Override
    @RestResource(exported = false)
    <S extends FtepFile> Iterable<S> save(Iterable<S> ftepFiles);

    @Override
    @PreAuthorize("#ftepFile.id != null and (hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#ftepFile, 'write'))")
    <S extends FtepFile> S save(@P("ftepFile") S ftepFile);

    @PostFilter("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(filterObject, 'read')")
    List<FtepFile> findByType(@Param("type") FtepFileType type);

    @PostAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(returnObject, 'read')")
    FtepFile findOneByUri(@Param("uri") URI uri);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    void delete(Iterable<? extends FtepFile> ftepFiles);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#ftepFile, 'administration')")
    void delete(@P("ftepFile") FtepFile ftepFile);

}
