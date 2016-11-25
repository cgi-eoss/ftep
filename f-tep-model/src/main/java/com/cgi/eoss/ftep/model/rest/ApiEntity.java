package com.cgi.eoss.ftep.model.rest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiEntity<T> {

    private T resource;
    private String resourceId;
    private String resourceEndpoint;

}
