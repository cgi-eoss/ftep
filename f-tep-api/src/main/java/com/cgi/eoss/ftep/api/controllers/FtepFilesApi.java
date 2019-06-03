package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortFtepFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RepositoryRestResource(path = "ftepFiles", itemResourceRel = "ftepFile", collectionResourceRel = "ftepFiles", excerptProjection = ShortFtepFile.class)
public interface FtepFilesApi extends FtepFilesApiCustom, PagingAndSortingRepository<FtepFile, Long> {

    @Override
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject.get(), 'read')")
    Optional<FtepFile> findById(Long id);

    @Override
    @RestResource(exported = false)
    <S extends FtepFile> Iterable<S> saveAll(Iterable<S> ftepFiles);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#ftepFile.id != null and hasPermission(#ftepFile, 'write'))")
    <S extends FtepFile> S save(@Param("ftepFile") S ftepFile);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void deleteAll(Iterable<? extends FtepFile> ftepFiles);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#ftepFile, 'administration')")
    void delete(@Param("ftepFile") FtepFile ftepFile);

    @RestResource(path = "findOneByUri", rel = "findOneByUri")
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject.get(), 'read')")
    Optional<FtepFile> findOneByUri(@Param("uri") URI uri);

    @RestResource(path = "findOneByRestoId", rel = "findOneByRestoId")
    @PostAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(returnObject.get(), 'read')")
    Optional<FtepFile> findOneByRestoId(@Param("uuid") UUID uuid);

    @Override
    @RestResource(path = "findByType", rel = "findByType")
    Page<FtepFile> searchByType(@Param("type") FtepFile.Type type, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterOnly", rel = "findByFilterOnly")
    Page<FtepFile> searchByFilterOnly(@Param("filter") String filter, @Param("type") FtepFile.Type type, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndOwner", rel = "findByFilterAndOwner")
    Page<FtepFile> searchByFilterAndOwner(@Param("filter") String filter, @Param("type") FtepFile.Type type, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findByFilterAndNotOwner", rel = "findByFilterAndNotOwner")
    Page<FtepFile> searchByFilterAndNotOwner(@Param("filter") String filter, @Param("type") FtepFile.Type type, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path = "findAll", rel = "findAll")
    Page<FtepFile> searchAll(@Param("filter") String filter,
                             @Param("type") FtepFile.Type type,
                             @Param("owner") User owner,
                             @Param("notOwner") User notOwner,
                             @Param("minFilesize") Long minFilesize,
                             @Param("maxFilesize") Long maxFilesize,
                             Pageable pageable);
}
