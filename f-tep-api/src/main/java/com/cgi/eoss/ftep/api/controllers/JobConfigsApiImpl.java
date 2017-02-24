package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.persistence.dao.JobConfigDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class JobConfigsApiImpl implements JobConfigsApiInferringOwner {

    private final FtepSecurityUtil ftepSecurityUtil;
    private final JobConfigDao jobConfigDao;

    @Autowired
    public JobConfigsApiImpl(FtepSecurityUtil ftepSecurityUtil, JobConfigDao jobConfigDao) {
        this.ftepSecurityUtil = ftepSecurityUtil;
        this.jobConfigDao = jobConfigDao;
    }

    @Override
    public <S extends JobConfig> S save(S jobConfig) {
        if (jobConfig.getOwner() == null) {
            ftepSecurityUtil.updateOwnerWithCurrentUser(jobConfig);
        }
        return jobConfigDao.save(jobConfig);
    }
}
