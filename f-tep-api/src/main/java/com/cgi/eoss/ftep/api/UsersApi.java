package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.UserDao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(path = "users", itemResourceRel = "user", collectionResourceRel = "users")
public interface UsersApi extends PagingAndSortingRepository<User, Long>, UserDao {

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
