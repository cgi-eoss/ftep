package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.ApiKey;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.ApiKeyDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.cgi.eoss.ftep.model.QApiKey.apiKey;

@Service
@Transactional(readOnly = true)
public class JpaApiKeyDataService extends AbstractJpaDataService<ApiKey> implements ApiKeyDataService {

    private final ApiKeyDao dao;

    @Autowired
    public JpaApiKeyDataService(ApiKeyDao apiKeyDao) {
        this.dao = apiKeyDao;
    }

    @Override
    FtepEntityDao<ApiKey> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(ApiKey entity) {
        return apiKey.owner.eq(entity.getOwner());
    }

    @Override
    public ApiKey getByOwner(User user) {
        return dao.getByOwner(user);
    }

}
