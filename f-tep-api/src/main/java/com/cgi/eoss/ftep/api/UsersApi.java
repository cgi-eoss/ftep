package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.dao.FtepUserDao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(path = "users", itemResourceRel = "user", collectionResourceRel = "users")
public interface UsersApi extends PagingAndSortingRepository<FtepUser, Long>, FtepUserDao {

    @Override
    @RestResource(exported = false)
    void delete(Iterable<? extends FtepUser> users);

    @Override
    @RestResource(exported = false)
    void delete(FtepUser user);

    @Override
    @RestResource(exported = false)
    void delete(Long id);

}
