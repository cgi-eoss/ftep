package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;

public interface JobsApiInferringOwner {
    <S extends Job> S save(S job);
}
