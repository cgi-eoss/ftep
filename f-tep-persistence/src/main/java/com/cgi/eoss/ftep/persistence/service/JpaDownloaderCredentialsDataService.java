package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.DownloaderCredentials;
import com.cgi.eoss.ftep.persistence.dao.DownloaderCredentialsDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.cgi.eoss.ftep.model.QDownloaderCredentials.downloaderCredentials;

@Service
@Transactional(readOnly = true)
public class JpaDownloaderCredentialsDataService extends AbstractJpaDataService<DownloaderCredentials> implements DownloaderCredentialsDataService {

    private final DownloaderCredentialsDao downloaderCredentialsDao;

    @Autowired
    public JpaDownloaderCredentialsDataService(DownloaderCredentialsDao downloaderCredentialsDao) {
        this.downloaderCredentialsDao = downloaderCredentialsDao;
    }

    @Override
    FtepEntityDao<DownloaderCredentials> getDao() {
        return downloaderCredentialsDao;
    }

    @Override
    Predicate getUniquePredicate(DownloaderCredentials entity) {
        return downloaderCredentials.host.eq(entity.getHost());
    }

    @Override
    public Optional<DownloaderCredentials> getByHost(String host) {
        return downloaderCredentialsDao.findOne(downloaderCredentials.host.eq(host));
    }

}
