package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.persistence.dao.JobDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class JobsApiImpl implements JobsApiInferringOwner {

    private final FtepSecurityUtil ftepSecurityUtil;
    private final JobDao jobDao;

    @Autowired
    public JobsApiImpl(FtepSecurityUtil ftepSecurityUtil, JobDao jobDao) {
        this.ftepSecurityUtil = ftepSecurityUtil;
        this.jobDao = jobDao;
    }

    @Override
    public <S extends Job> S save(S job) {
        if (job.getOwner() == null) {
            ftepSecurityUtil.updateOwnerWithCurrentUser(job);
        }
        return jobDao.save(job);
    }
}
