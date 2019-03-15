package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.converters.StringListConverter;
import com.cgi.eoss.ftep.model.converters.StringMultimapYamlConverter;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.JobConfigDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class JpaJobConfigDataService extends AbstractJpaDataService<JobConfig> implements JobConfigDataService {

    private final JobConfigDao dao;
    private final EntityManager em;
    private final StringMultimapYamlConverter stringMultimapYamlConverter;
    private final StringListConverter stringListConverter;

    @Autowired
    public JpaJobConfigDataService(JobConfigDao jobConfigDao, EntityManager em) {
        this.dao = jobConfigDao;
        this.em = em;
        this.stringMultimapYamlConverter = new StringMultimapYamlConverter();
        this.stringListConverter = new StringListConverter();
    }

    @Override
    FtepEntityDao<JobConfig> getDao() {
        return dao;
    }

    @Override
    protected Optional<JobConfig> findOne(JobConfig entity) {
        String queryString = "select jc.* from ftep_job_configs jc where " +
                " jc.owner = :owner" +
                " and jc.service = :service" +
                " and jc.inputs = :inputs" +
                " and jc.parallel_parameters = :parallel_parameters" +
                " and jc.search_parameters = :search_parameters";

        Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put("owner", entity.getOwner().getId());
        queryParameters.put("service", entity.getService().getId());
        queryParameters.put("inputs", stringMultimapYamlConverter.convertToDatabaseColumn(entity.getInputs()));
        queryParameters.put("parallel_parameters", stringListConverter.convertToDatabaseColumn(entity.getParallelParameters()));
        queryParameters.put("search_parameters", stringListConverter.convertToDatabaseColumn(entity.getSearchParameters()));

        if (entity.getParent() != null) {
            queryString += " and jc.parent = :parent";
            queryParameters.put("parent", entity.getParent().getId());
        }

        if (entity.getSystematicParameter() != null) {
            queryString += " and jc.systematic_parameter = :systematic_parameter";
            queryParameters.put("systematic_parameter", entity.getSystematicParameter());
        }

        Query query = em.createNativeQuery(queryString, JobConfig.class);
        queryParameters.forEach(query::setParameter);
        List<JobConfig> results = query.getResultList();

        if (results.size() > 1) {
            throw new org.springframework.dao.IncorrectResultSizeDataAccessException(1, results.size());
        }
        return results.stream().findFirst();
    }

    @Override
    Predicate getUniquePredicate(JobConfig entity) {
        throw new UnsupportedOperationException("QueryDSL Predicate is insufficient to locate unique JobConfig");

    }

    @Override
    public List<JobConfig> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<JobConfig> findByService(FtepService service) {
        return dao.findByService(service);
    }

    @Override
    public List<JobConfig> findByOwnerAndService(User user, FtepService service) {
        return dao.findByOwnerAndService(user, service);
    }

}
