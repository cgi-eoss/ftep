package com.cgi.eoss.ftep.orchestrator;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Set;

/**
 */
@Data
@Builder
public class DockerLaunchConfig {

    private String image;

    @Singular
    private Set<String> volumes;

    @Singular
    private Set<String> binds;

    @Singular
    private Set<String> exposedPorts;

    private boolean defaultLogging;

}
