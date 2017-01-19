package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.FtepDatabasket;
import com.cgi.eoss.ftep.persistence.dao.FtepDatabasketDao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "databaskets", itemResourceRel = "databasket", collectionResourceRel = "databaskets")
public interface DatabasketsApi extends PagingAndSortingRepository<FtepDatabasket, Long>, FtepDatabasketDao {
}
