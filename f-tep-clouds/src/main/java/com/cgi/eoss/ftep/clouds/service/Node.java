package com.cgi.eoss.ftep.clouds.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Node {
    private final String id;
    private final String name;
    private final String dockerEngineUrl;
}
