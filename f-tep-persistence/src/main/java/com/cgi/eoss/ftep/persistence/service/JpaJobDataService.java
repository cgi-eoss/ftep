package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.JobDao;

import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

import static com.cgi.eoss.ftep.model.QJob.job;

@Service
@Transactional(readOnly = true)
public class JpaJobDataService extends AbstractJpaDataService<Job> implements JobDataService {

    private final JobDao dao;

    private final JobConfigDataService jobConfigDataService;

    private final UserDataService userDataService;

    private final ServiceDataService serviceDataService;

    @Autowired
    public JpaJobDataService(JobDao jobDao, JobConfigDataService jobConfigDataService, UserDataService userDataService, ServiceDataService serviceDataService) {
        this.dao = jobDao;
        this.jobConfigDataService = jobConfigDataService;
        this.userDataService = userDataService;
        this.serviceDataService = serviceDataService;
    }

    @Override
    FtepEntityDao<Job> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(Job entity) {
        return job.extId.eq(entity.getExtId());
    }

    @Override
    public List<Job> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<Job> findByService(FtepService service) {
        return dao.findByConfig_Service(service);
    }

    @Override
    public List<Job> findByOwnerAndService(User user, FtepService service) {
        return dao.findByOwnerAndConfig_Service(user, service);
    }

    @Override
    public List<Job> findByStartIn(YearMonth yearMonth) {
        return dao.findAll(job.startTime.year().eq(yearMonth.getYear()).and(job.startTime.month().eq(yearMonth.getMonthValue())));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Job buildNew(String extId, String ownerId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs, Job parentJob) {
        User owner = userDataService.getByName(ownerId);
        FtepService service = serviceDataService.getByName(serviceId);

        JobConfig config = new JobConfig(owner, service);
        config.setLabel(Strings.isNullOrEmpty(jobConfigLabel) ? null : jobConfigLabel);
        config.setInputs(inputs);
        config.setParent(parentJob);
        return buildNew(jobConfigDataService.save(config), extId, owner, parentJob);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Job buildNew(String extId, String ownerId, String serviceId, String jobConfigLabel, Multimap<String, String> inputs) {
        return buildNew(extId, ownerId, serviceId, jobConfigLabel, inputs, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Job updateJobConfig(Job job) {
        jobConfigDataService.save(job.getConfig());
        return job;
    }

    private Job buildNew(JobConfig jobConfig, String extId, User owner, Job parentJob) {
        return dao.save(new Job(jobConfig, extId, owner, parentJob));
    }
}
