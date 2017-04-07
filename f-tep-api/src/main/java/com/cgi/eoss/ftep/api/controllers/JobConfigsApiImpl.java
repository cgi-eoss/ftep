package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.QJobConfig;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.persistence.dao.JobConfigDao;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class JobConfigsApiImpl extends BaseRepositoryApiImpl<JobConfig> {

    private final FtepSecurityService securityService;
    private final JobConfigDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QJobConfig.jobConfig.id;
    }

    @Override
    QUser getOwnerPath() {
        return QJobConfig.jobConfig.owner;
    }

    @Override
    Class<JobConfig> getEntityClass() {
        return JobConfig.class;
    }

}
