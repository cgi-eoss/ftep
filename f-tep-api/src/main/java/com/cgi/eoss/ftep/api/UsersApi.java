package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(path = "users", itemResourceRel = "user", collectionResourceRel = "users")
public interface UsersApi extends CrudRepository<User, Long> {

    @Override
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    <S extends User> S save(@P("user") S user);

    @Override
    @RestResource(exported = false)
    void delete(Iterable<? extends User> users);

    @Override
    @RestResource(exported = false)
    void delete(User user);

    @Override
    @RestResource(exported = false)
    void delete(Long id);

}
