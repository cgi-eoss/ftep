package com.cgi.eoss.ftep.orchestrator.data;

import com.cgi.eoss.ftep.model.JobStatus;
import com.cgi.eoss.ftep.model.rest.ApiEntity;
import com.cgi.eoss.ftep.model.rest.ResourceJob;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

// TODO Remove this when direct java API is functional
@Slf4j
public class JobStatusService {

    private final FtepJsonApi api;

    public JobStatusService(FtepJsonApi api) {
        this.api = api;
    }

    public ApiEntity<ResourceJob> create(String jobId, String userId, String serviceName) throws IOException, IllegalAccessException {
        ResourceJob job = ResourceJob.builder()
                .jobId(jobId)
                .userId(userId)
                .serviceName(serviceName)
                .inputs("\"{}\"")
                .outputs("\"{}\"")
                .guiEndpoint(null)
                .step("\"{}\"")
                .status(JobStatus.CREATED.name())
                .build();

        ApiEntity<ResourceJob> apiEntity = api.insert(job);
        LOG.debug("Created ResourceJob entity with id {} at endpoint {}", apiEntity.getResourceId(), apiEntity.getResourceEndpoint());
        return apiEntity;
    }


    public void update(ApiEntity<ResourceJob> apiJob) throws IOException, IllegalAccessException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(apiJob.getResourceEndpoint()));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(apiJob.getResourceId()));

        api.update(apiJob);
    }


    public void setJobInError(ApiEntity<ResourceJob> apiJob) {
        try {
            apiJob.getResource().setStatus(JobStatus.ERROR.name());
            update(apiJob);
        } catch (Exception e) {
            LOG.error("Unable to set job to ERROR state (swallowing exception)", e);
        }
    }

}
