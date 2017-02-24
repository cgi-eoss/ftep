package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepService;

public interface ServicesApiInferringOwner {
    <S extends FtepService> S save(S service);
}
