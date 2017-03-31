package com.cgi.eoss.ftep.api.projections;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobStatus;
import org.springframework.data.rest.core.config.Projection;

import java.time.LocalDateTime;

/**
 * <p>Default JSON projection for embedded {@link Job}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortFtepService", types = {Job.class})
public interface ShortJob extends EmbeddedId {
    String getExtId();
    ShortUser getOwner();
    JobStatus getStatus();
    String getGuiUrl();
    String getStage();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
}
