package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.CostingExpression;
import com.cgi.eoss.ftep.persistence.dao.CostingExpressionDao;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

}
