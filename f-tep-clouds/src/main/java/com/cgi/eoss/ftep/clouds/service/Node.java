package com.cgi.eoss.ftep.clouds.service;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = {"id"})
@Builder
public class Node {
    private final String id;
    private final String name;
    private final String tag;
    private final long creationEpochSecond;
    private final String dockerEngineUrl;
    private final String ipAddress;
}
