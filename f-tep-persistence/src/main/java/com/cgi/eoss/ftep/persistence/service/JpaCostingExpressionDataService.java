package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.CostingExpression;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.persistence.dao.CostingExpressionDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.cgi.eoss.ftep.model.QCostingExpression.costingExpression;

@Service
@Transactional(readOnly = true)
public class JpaCostingExpressionDataService extends AbstractJpaDataService<CostingExpression> implements CostingExpressionDataService {

    private final CostingExpressionDao costingExpressionDao;

    @Autowired
    public JpaCostingExpressionDataService(CostingExpressionDao costingExpressionDao) {
        this.costingExpressionDao = costingExpressionDao;
    }

    @Override
    FtepEntityDao<CostingExpression> getDao() {
        return costingExpressionDao;
    }

    @Override
    Predicate getUniquePredicate(CostingExpression entity) {
        return costingExpression.type.eq(entity.getType()).and(costingExpression.associatedId.eq(entity.getAssociatedId()));
    }

    @Override
    public Optional<CostingExpression> getServiceCostingExpression(FtepService service) {
        return costingExpressionDao.findOne(
                costingExpression.type.eq(CostingExpression.Type.SERVICE)
                        .and(costingExpression.associatedId.eq(service.getId())));
    }

    @Override
    public Optional<CostingExpression> getDownloadCostingExpression(FtepFile ftepFile) {
        if (ftepFile.getDataSource() == null) {
            return Optional.empty();
        }

        return costingExpressionDao.findOne(
                costingExpression.type.eq(CostingExpression.Type.DOWNLOAD)
                        .and(costingExpression.associatedId.eq(ftepFile.getDataSource().getId())));
    }

}
