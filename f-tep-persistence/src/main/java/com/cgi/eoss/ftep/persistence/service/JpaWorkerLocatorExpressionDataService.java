package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.WorkerLocatorExpression;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.WorkerLocatorExpressionDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.cgi.eoss.ftep.model.QWorkerLocatorExpression.workerLocatorExpression;

@Service
@Transactional(readOnly = true)
public class JpaWorkerLocatorExpressionDataService extends AbstractJpaDataService<WorkerLocatorExpression> implements WorkerLocatorExpressionDataService {

    private final WorkerLocatorExpressionDao workerLocatorExpressionDao;

    @Autowired
    public JpaWorkerLocatorExpressionDataService(WorkerLocatorExpressionDao workerLocatorExpressionDao) {
        this.workerLocatorExpressionDao = workerLocatorExpressionDao;
    }

    @Override
    FtepEntityDao<WorkerLocatorExpression> getDao() {
        return workerLocatorExpressionDao;
    }

    @Override
    Predicate getUniquePredicate(WorkerLocatorExpression entity) {
        return workerLocatorExpression.service.eq(entity.getService());
    }

    @Override
    public WorkerLocatorExpression getByService(FtepService service) {
        return workerLocatorExpressionDao.findOneByService(service);
    }

}
