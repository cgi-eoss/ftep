package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Databasket;

public interface DatabasketsApiInferringOwner {
    <S extends Databasket> S save(S databasket);
}
