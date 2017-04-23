package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.projections.ShortUser;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(path = "users", itemResourceRel = "user", collectionResourceRel = "users", excerptProjection = ShortUser.class)
public interface UsersApi extends PagingAndSortingRepository<User, Long> {

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    <S extends User> Iterable<S> save(Iterable<S> users);

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    <S extends User> S save(@P("user") S user);

    @Override
    @RestResource(exported = false)
    void delete(Iterable<? extends User> users);

    @Override
    @RestResource(exported = false)
    void delete(User user);

}
