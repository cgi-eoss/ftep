package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Group;

public interface GroupsApiInferringOwner {
    <S extends Group> S save(S group);
}
