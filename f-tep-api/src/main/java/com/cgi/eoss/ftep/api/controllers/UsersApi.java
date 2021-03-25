package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Optional;

@PreAuthorize("hasRole('ADMIN')")
@RepositoryRestResource(path = "users", itemResourceRel = "user", collectionResourceRel = "users", excerptProjection = ShortUser.class)
public interface UsersApi extends PagingAndSortingRepository<User, Long> {

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','CONTENT_AUTHORITY', 'EXPERT_USER', 'USER', 'GUEST')")
    Optional<User> findById(@Param("id") Long id);

    @Override
    <S extends User> Iterable<S> saveAll(Iterable<S> users);

    @Override
    <S extends User> S save(@Param("user") S user);

    @Override
    @RestResource(exported = false)
    void deleteAll(Iterable<? extends User> users);

    @Override
    @RestResource(exported = false)
    void delete(User user);

    @RestResource(path = "byName", rel = "byName")
    Page<User> findByNameContainsIgnoreCase(@Param("name") String name, Pageable pageable);

    @RestResource(path="byFilter", rel="byFilter")
    @Query("select u from User u where lower(u.name) like concat('%', lower(:filter), '%') or lower(u.email) like concat('%', lower(:filter), '%')")
    Page<User> findByFilter(@Param("filter") String filter, Pageable pageable);

    @PreAuthorize("hasAnyRole('ADMIN','CONTENT_AUTHORITY', 'EXPERT_USER', 'USER', 'GUEST')")
    @RestResource(path="byFilterExact", rel="byFilterExact")
    @Query("select u from User u where lower(u.name) = lower(:filter) or lower(u.email) = lower(:filter)")
    Page<User> findByFilterExact(@Param("filter") String filter, Pageable pageable);

}
