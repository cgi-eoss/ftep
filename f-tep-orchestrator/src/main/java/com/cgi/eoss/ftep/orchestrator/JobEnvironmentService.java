package com.cgi.eoss.ftep.orchestrator;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>Service to manage working environments for WPS job instances, including directory handling.</p>
 */
@Slf4j
public class JobEnvironmentService {

    private static final Path DEFAULT_BASEDIR = Paths.get("/data/cache");

    public JobEnvironment createEnvironment(String jobId) throws IOException {
        Path workingDir = getBasedir().resolve("Job_" + jobId);
        Path inputDir = workingDir.resolve("inDir");
        Path outputDir = workingDir.resolve("outDir");

        Files.createDirectory(workingDir);
        Files.createDirectory(inputDir);
        Files.createDirectory(outputDir);

        LOG.info("Created working environment for job {} in location: {}", jobId, workingDir);

        return JobEnvironment.builder()
                .jobId(jobId)
                .workingDir(workingDir)
                .inputDir(inputDir)
                .outputDir(outputDir)
                .build();
    }

    // TODO Make this configurable
    public Path getBasedir() {
        return DEFAULT_BASEDIR;
    }
}
