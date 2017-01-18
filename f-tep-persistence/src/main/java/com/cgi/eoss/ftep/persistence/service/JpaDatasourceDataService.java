package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepDatasource;
import com.cgi.eoss.ftep.model.internal.Credentials;
import com.cgi.eoss.ftep.persistence.dao.FtepDatasourceDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.querydsl.core.types.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepDatasource.ftepDatasource;

@Service
@Transactional(readOnly = true)
@Slf4j
public class JpaDatasourceDataService extends AbstractJpaDataService<FtepDatasource> implements DatasourceDataService {

    private static final String POLICY_CREDENTIALS = "credentials";
    private static final String POLICY_X509 = "x509";
    private final JsonParser jsonParser = new JsonParser();

    private final FtepDatasourceDao ftepDatasourceDao;

    @Autowired
    public JpaDatasourceDataService(FtepDatasourceDao ftepDatasourceDao) {
        this.ftepDatasourceDao = ftepDatasourceDao;
    }

    @Override
    FtepEntityDao<FtepDatasource> getDao() {
        return ftepDatasourceDao;
    }

    @Override
    Predicate getUniquePredicate(FtepDatasource entity) {
        return ftepDatasource.name.eq(entity.getName());
    }

    @Override
    public List<FtepDatasource> search(String term) {
        return ftepDatasourceDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public Credentials getCredentials(String host) {
        FtepDatasource datasource = ftepDatasourceDao.findOne(ftepDatasource.downloadDomain.eq(host));
        Credentials.CredentialsBuilder retBuilder = Credentials.builder();

        String policy = datasource.getPolicy();
        if (!Strings.isNullOrEmpty(policy)) {
            String credentialsString = datasource.getCredentialsData();
            JsonObject credentials = jsonParser.parse(credentialsString).getAsJsonObject();

            switch (policy) {
                case POLICY_CREDENTIALS:
                    retBuilder
                            .username(credentials.get("user").getAsString())
                            .password(credentials.get("password").getAsString());
                    break;
                case POLICY_X509:
                    retBuilder
                            .certificate(credentials.get("certpath").getAsString());
                    break;
                default:
                    LOG.trace("Unrecognised credentials policy ({}) found for host: {}", policy, host);
                    break;
            }
        }

        return retBuilder.build();
    }
}
