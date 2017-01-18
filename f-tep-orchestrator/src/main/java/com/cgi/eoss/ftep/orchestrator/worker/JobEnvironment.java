package com.cgi.eoss.ftep.orchestrator.worker;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

@Data
@Builder
public class JobEnvironment {
    private String jobId;
    private Path workingDir;
    private Path inputDir;
    private Path outputDir;
}
