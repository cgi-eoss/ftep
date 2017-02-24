package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.JobConfig;

public interface JobConfigsApiInferringOwner {
    <S extends JobConfig> S save(S jobConfig);
}
