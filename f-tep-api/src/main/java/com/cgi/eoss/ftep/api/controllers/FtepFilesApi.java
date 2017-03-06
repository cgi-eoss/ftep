package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.projections.ShortFtepFile;
import com.cgi.eoss.ftep.model.FtepFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(
        path = "ftepFiles",
        itemResourceRel = "ftepFile",
        collectionResourceRel = "ftepFiles",
        excerptProjection = ShortFtepFile.class)
public interface FtepFilesApi extends CrudRepository<FtepFile, Long> {

    @Override
    @RestResource(exported = false)
    <S extends FtepFile> Iterable<S> save(Iterable<S> ftepFiles);

    @Override
    @PreAuthorize("#ftepFile.id != null and (hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#ftepFile, 'write'))")
    <S extends FtepFile> S save(@P("ftepFile") S ftepFile);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    void delete(Iterable<? extends FtepFile> ftepFiles);

    @Override
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#ftepFile, 'administration')")
    void delete(@P("ftepFile") FtepFile ftepFile);

}
