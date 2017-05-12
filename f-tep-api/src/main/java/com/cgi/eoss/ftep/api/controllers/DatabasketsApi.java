package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortDatabasket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RepositoryRestResource(
        path = "databaskets",
        itemResourceRel = "databasket",
        collectionResourceRel = "databaskets",
        excerptProjection = ShortDatabasket.class)
public interface DatabasketsApi extends BaseRepositoryApi<Databasket>, DatabasketsApiCustom, PagingAndSortingRepository<Databasket, Long> {

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    <S extends Databasket> Iterable<S> save(Iterable<S> databaskets);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#databasket.id == null) or hasPermission(#databasket, 'write')")
    <S extends Databasket> S save(@P("databasket") S databasket);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    void delete(Iterable<? extends Databasket> databaskets);

    @Override
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#databasket, 'administration')")
    void delete(@P("databasket") Databasket databasket);

    @Override
    @Query("select t from Databasket t where t.owner=user")
    Page<Databasket> findByOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @Query("select t from Databasket t where not t.owner=user")
    Page<Databasket> findByNotOwner(@Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path="findByFilterOnly", rel="findByFilterOnly")
    @Query("select t from Databasket t where t.name like CONCAT('%', filter, '%') or t.description like CONCAT('%', filter, '%')")
    Page<Databasket> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(@Param("filter") String filter, Pageable pageable);

    @Override
    @RestResource(path="findByFilterAndOwner", rel="findByFilterAndOwner")
    @Query("select t from Databasket t where t.owner=user and (t.name like CONCAT('%', filter, '%') or t.description like CONCAT('%', filter, '%'))")
    Page<Databasket> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndOwner(@Param("filter") String filter, @Param("owner") User user, Pageable pageable);

    @Override
    @RestResource(path="findByFilterAndNotOwner", rel="findByFilterAndNotOwner")
    @Query("select t from Databasket t where not t.owner=user and (t.name like CONCAT('%', filter, '%') or t.description like CONCAT('%', filter, '%'))")
    Page<Databasket> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseAndNotOwner(@Param("filter") String filter, @Param("owner") User user, Pageable pageable);

}
