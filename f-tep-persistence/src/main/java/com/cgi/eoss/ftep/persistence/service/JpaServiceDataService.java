package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.internal.CompleteFtepService;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.FtepServiceDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.ftep.model.QFtepService.ftepService;

@Service
@Transactional(readOnly = true)
public class JpaServiceDataService extends AbstractJpaDataService<FtepService> implements ServiceDataService {

    private final FtepServiceDao ftepServiceDao;

    private final UserDataService userDataService;

    private final ServiceFileDataService fileDataService;

    @Autowired
    public JpaServiceDataService(FtepServiceDao ftepServiceDao, UserDataService userDataService, ServiceFileDataService fileDataService) {
        this.ftepServiceDao = ftepServiceDao;
        this.userDataService = userDataService;
        this.fileDataService = fileDataService;
    }

    @Override
    FtepEntityDao<FtepService> getDao() {
        return ftepServiceDao;
    }

    @Override
    Predicate getUniquePredicate(FtepService entity) {
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
        return ftepServiceDao.findOne(ftepService.name.eq(serviceName));
    }

    @Override
    public CompleteFtepService save(CompleteFtepService service) {
        FtepService ftepService = service.getService();
        ftepService.setOwner(userDataService.refresh(ftepService.getOwner()));
        save(ftepService);
        fileDataService.save(service.getFiles());
        return service;
    }

    @Override
    public void delete(FtepService service) {
        // TODO Fix the relationship so we can use orphanRemoval
        fileDataService.findByService(service).forEach(fileDataService::delete);
        ftepServiceDao.delete(service);
    }

}
