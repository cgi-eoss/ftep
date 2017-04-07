package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.QJob;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.persistence.dao.JobDao;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class JobsApiImpl extends BaseRepositoryApiImpl<Job> {

    private final FtepSecurityService securityService;
    private final JobDao dao;

    @Override
    NumberPath<Long> getIdPath() {
        return QJob.job.id;
    }

    @Override
    QUser getOwnerPath() {
        return QJob.job.owner;
    }

    @Override
    Class<Job> getEntityClass() {
        return Job.class;
    }

}
