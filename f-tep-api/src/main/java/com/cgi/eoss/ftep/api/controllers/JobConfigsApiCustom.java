package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.JobConfig;

public interface JobConfigsApiCustom extends BaseRepositoryApi<JobConfig> {
    <S extends JobConfig> S save(S entity);
}
