package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Project;

public interface ProjectsApiInferringOwner {
    <S extends Project> S save(S project);
}
