package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.security.FtepAccess;
import com.cgi.eoss.ftep.model.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.time.LocalDateTime;

/**
 * <p>Default JSON projection for embedded {@link Job}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortFtepService", types = {Job.class})
public interface ShortJob extends Identifiable<Long> {
    String getExtId();
    ShortUser getOwner();
    Job.Status getStatus();
    String getGuiUrl();
    String getStage();
    @Value("#{target.config.label}")
    String getLabel();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    @Value("#{target.config.service.name}")
    String getServiceName();
    @Value("#{@ftepSecurityService.getCurrentAccess(T(com.cgi.eoss.ftep.model.Job), target.id)}")
    FtepAccess getAccess();
}
