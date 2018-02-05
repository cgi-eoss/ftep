package com.cgi.eoss.ftep.worker.worker;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

/**
 * <p>Describes the workspace for an F-TEP job execution.</p>
 */
@Data
@Builder
class JobEnvironment {
    /**
     * <p>The identifier of the job using this environment.</p>
     */
    private String jobId;

    /**
     * <p>The in-process workspace, used for temporary files created during service execution.</p>
     */
    private Path workingDir;

    /**
     * <p>The input workspace, pre-populated by the {@link FtepWorker} before service execution.</p>
     */
    private Path inputDir;

    /**
     * <p>The output workspace, used for end-result files created during service execution. The {@link FtepWorker}
     * collects from this path.</p>
     */
    private Path outputDir;

    /**
     * <p>The temporary workspace, used for interim files created during service execution. The {@link FtepWorker}
     * should delete this path after execution.</p>
     */
    private Path tempDir;
}
