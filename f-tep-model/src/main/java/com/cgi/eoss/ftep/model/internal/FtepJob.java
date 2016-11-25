package com.cgi.eoss.ftep.model.internal;

import com.cgi.eoss.ftep.model.FtepEntity;
import com.cgi.eoss.ftep.model.JobStatus;
import com.cgi.eoss.ftep.model.Searchable;
import com.google.common.collect.ComparisonChain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@Data
@Builder
@EqualsAndHashCode(exclude = {"id"})
@NoArgsConstructor
@AllArgsConstructor
public class FtepJob implements FtepEntity<FtepJob>, Searchable {
    /**
     * <p>Unique database identifier of the job. See also the jobId parameter set by ZOO.</p>
     */
    private Long id;

    /**
     * <p>Job UUID as provided by ZOO.</p>
     */
    private String jobId;

    /**
     * <p>User id as provided by ZOO.</p>
     */
    private String userId;

    /**
     * <p>Service id as provided by ZOO.</p>
     */
    private String serviceId;

    /**
     * <p>The location of the job workspace.</p>
     */
    private Path workingDir;

    /**
     * <p>The location of all inputs to this job.</p>
     */
    private Path inputDir;

    /**
     * <p>The location into which this job will place it output products.</p>
     */
    private Path outputDir;

    /**
     * <p>The job execution status.</p>
     */
    private JobStatus status;

    @Override
    public int compareTo(FtepJob o) {
        return ComparisonChain.start().compare(jobId, o.jobId).result();
    }

}
