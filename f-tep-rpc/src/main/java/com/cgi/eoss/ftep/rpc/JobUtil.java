package com.cgi.eoss.ftep.rpc;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

@Log4j2
@UtilityClass
public class JobUtil {

    private static final EnumSet<Job.Status> TERMINAL_STATUSES = EnumSet.of(
            Job.Status.CANCELLED,
            Job.Status.COMPLETED,
            Job.Status.FAILED
    );

    public static GetJobStatusResponse awaitJobTermination(String jobId, JobDataServiceGrpc.JobDataServiceBlockingStub jobDataService, Duration maxDuration) {
        RetryPolicy<GetJobStatusResponse> retryPolicy = new RetryPolicy<GetJobStatusResponse>()
                .handleResultIf(getJobStatusResponse -> {
                    LOG.debug("Got status: {}", getJobStatusResponse.getJobStatus());
                    return !TERMINAL_STATUSES.contains(getJobStatusResponse.getJobStatus());
                })
                .withBackoff(1, 32, ChronoUnit.SECONDS)
                .withMaxDuration(maxDuration)
                .withMaxRetries(-1)
                .onRetry(e -> LOG.debug("Job {} does not appear to be in a terminal state yet ({} after {}), retrying", jobId, e.getLastResult().getJobStatus(), e.getElapsedTime()))
                .onRetriesExceeded(e -> LOG.warn("Failed to retrieve job status after {} for job {}", maxDuration, jobId))
                .onAbort(e -> LOG.warn("Aborting retrieving job status for job {}: {}", jobId, e));

        return Failsafe.with(retryPolicy)
                .get(() -> jobDataService.getJobStatus(GetJobStatusRequest.newBuilder().setJobId(jobId).build()));
    }

}
