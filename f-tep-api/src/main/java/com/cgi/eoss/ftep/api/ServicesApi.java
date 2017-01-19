package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceDao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(path = "services", itemResourceRel = "service", collectionResourceRel = "services")
public interface ServicesApi extends PagingAndSortingRepository<FtepService, Long>, FtepServiceDao {

    @Override
    @RestResource(exported = false)
    void delete(Iterable<? extends FtepService> services);

    @Override
    @RestResource(exported = false)
    void delete(FtepService service);

    @Override
    @RestResource(exported = false)
    void delete(Long id);

}
