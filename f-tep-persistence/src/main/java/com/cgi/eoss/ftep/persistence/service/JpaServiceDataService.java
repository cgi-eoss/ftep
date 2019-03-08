package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceDao;

import com.google.common.hash.Hashing;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepService.ftepService;

@Service
@Transactional(readOnly = true)
public class JpaServiceDataService extends AbstractJpaDataService<FtepService> implements ServiceDataService {

    private final FtepServiceDao ftepServiceDao;
    private final ServiceFileDataService serviceFilesDataService;

    @Autowired
    public JpaServiceDataService(FtepServiceDao ftepServiceDao, ServiceFileDataService serviceFilesDataService) {
        this.ftepServiceDao = ftepServiceDao;
        this.serviceFilesDataService = serviceFilesDataService;
    }

    @Override
    public FtepEntityDao<FtepService> getDao() {
        return ftepServiceDao;
    }

    @Override
    public Predicate getUniquePredicate(FtepService entity) {
        return ftepService.name.eq(entity.getName());
    }

    @Override
    public List<FtepService> search(String term) {
        return ftepServiceDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public List<FtepService> findByOwner(User user) {
        return ftepServiceDao.findByOwner(user);
    }

    @Override
    public FtepService getByName(String serviceName) {
        return ftepServiceDao.findOne(ftepService.name.eq(serviceName))
                .orElse(null); // TODO Return Optional<FtepService>
    }

    @Override
    public List<FtepService> findAllAvailable() {
        return ftepServiceDao.findByStatus(FtepService.Status.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public String computeServiceFingerprint(FtepService ftepService) {
        ObjectOutputStream oos;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            oos = new ObjectOutputStream(bos);
            List<FtepServiceContextFile> serviceFiles = serviceFilesDataService.findByService(ftepService);
            for (FtepServiceContextFile contextFile : serviceFiles) {
                oos.writeObject(contextFile.getFilename());
                oos.writeObject(contextFile.getContent());
            }
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] serviceSerialized = bos.toByteArray();
            digest.update(serviceSerialized);
            return Hashing.md5().hashBytes(digest.digest()).toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        }
    }
}
