package com.cgi.eoss.ftep.orchestrator;

import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE_NEW;


/**
 * <p>Service to manage working environments for WPS job instances, including directory handling.</p>
 */
@Slf4j
public class JobEnvironmentService {

    private static final Path DEFAULT_BASEDIR = Paths.get("/data/cache");
    private static final String JOB_CONFIG_FILENAME = "FTEP-WPS-INPUT.properties";
    private static final String WORKING_DIR_PREFIX = "Job_";
    private static final String INPUT_DIR = "inDir";
    private static final String OUTPUT_DIR = "outDir";

    /**
     * <p>Create a new job environment. If the jobConfig map is not empty, a job configuration file is created in the
     * workspace.</p>
     *
     * @param jobId The jobId for the environment.
     * @param jobConfig The job configuration parameters, to be written to {@link #JOB_CONFIG_FILENAME} in the working
     * directory if not empty.
     * @return The created environment.
     * @throws IOException If any problem occurred in creating the job workspace or config file.
     */
    public JobEnvironment createEnvironment(String jobId, Multimap<String, String> jobConfig) throws IOException {
        JobEnvironment environment = createEnvironment(jobId);

        if (!jobConfig.isEmpty()) {
            Path configFile = environment.getWorkingDir().resolve(JOB_CONFIG_FILENAME);

            List<String> configFileLines = jobConfig.keySet().stream()
                    .map(key -> key + "=" + String.join(",", jobConfig.get(key)))
                    .collect(Collectors.toList());
            Files.write(configFile, configFileLines, CREATE_NEW);

            LOG.info("Created job configuration file for job {} in location: {}", jobId, configFile);
        }

        return environment;
    }

    // TODO Make this configurable
    public Path getBasedir() {
        return DEFAULT_BASEDIR;
    }

    /**
     * <p>Create a new job environment with the required workspace directories.</p>
     *
     * @param jobId The jobId for the environment.
     * @return The created environment.
     */
    private JobEnvironment createEnvironment(String jobId) throws IOException {
        Path workingDir = getBasedir().resolve(WORKING_DIR_PREFIX + jobId);
        Path inputDir = workingDir.resolve(INPUT_DIR);
        Path outputDir = workingDir.resolve(OUTPUT_DIR);

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

}
