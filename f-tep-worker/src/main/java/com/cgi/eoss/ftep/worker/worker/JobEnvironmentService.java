package com.cgi.eoss.ftep.worker.worker;

import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

/**
 * <p>Service to manage working environments for WPS job instances, including directory handling.</p>
 */
@Service
@Log4j2
public class JobEnvironmentService {

    private static final String JOB_CONFIG_FILENAME = "FTEP-WPS-INPUT.properties";
    private static final String WORKING_DIR_PREFIX = "Job_";
    private static final String INPUT_DIR = "inDir";
    private static final String OUTPUT_DIR = "outDir";
    private static final String TEMP_DIR = "procDir";

    @Getter
    private final Path baseDir;

    /**
     * <p>Construct a new JobEnvironmentService which manages job workspaces in the given base directory.</p>
     *
     * @param jobEnvironmentRoot The absolute path to the job workspace parent directory.
     */
    @Autowired
    public JobEnvironmentService(@Qualifier("jobEnvironmentRoot") Path jobEnvironmentRoot) {
        this.baseDir = jobEnvironmentRoot;
    }

    /**
     * <p>Create a new job environment. If the config map is not empty, a job configuration file is created in the
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
                    .map(key -> key + "=" + StringUtils.wrapIfMissing(String.join(",", jobConfig.get(key)), '"'))
                    .collect(Collectors.toList());
            Files.write(configFile, configFileLines, CREATE_NEW);

            LOG.info("Created job configuration file for job {} in location: {}", jobId, configFile);
        }

        return environment;
    }

    /**
     * <p>Create a new job environment with the required workspace directories.</p>
     *
     * @param jobId The jobId for the environment.
     * @return The created environment.
     */
    private JobEnvironment createEnvironment(String jobId) throws IOException {
        Path workingDir = getBaseDir().resolve(WORKING_DIR_PREFIX + jobId);
        Path inputDir = workingDir.resolve(INPUT_DIR);
        Path outputDir = workingDir.resolve(OUTPUT_DIR);
        Path tempDir = workingDir.resolve(TEMP_DIR);

        Files.createDirectory(workingDir);
        Files.createDirectory(inputDir);
        // TODO Tighten permissions when service uids/gids are controlled
        Files.createDirectory(outputDir, PosixFilePermissions.asFileAttribute(EnumSet.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE)));
        Files.createDirectory(tempDir, PosixFilePermissions.asFileAttribute(EnumSet.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE)));

        LOG.info("Created working environment for job {} in location: {}", jobId, workingDir);

        return JobEnvironment.builder()
                .jobId(jobId)
                .workingDir(workingDir)
                .inputDir(inputDir)
                .outputDir(outputDir)
                .tempDir(tempDir)
                .build();
    }

    /**
     * Return a JobEnvironment object for a job for which an environment has already been created
     *
     * @param jobId
     * @return
     */
    public JobEnvironment getExistingJobEnvironment(String jobId) {
        Path workingDir = getBaseDir().resolve(WORKING_DIR_PREFIX + jobId);
        return JobEnvironment.builder()
                .jobId(jobId)
                .workingDir(workingDir)
                .inputDir(workingDir.resolve(INPUT_DIR))
                .outputDir(workingDir.resolve(OUTPUT_DIR))
                .tempDir(workingDir.resolve(TEMP_DIR))
                .build();
    }

}
