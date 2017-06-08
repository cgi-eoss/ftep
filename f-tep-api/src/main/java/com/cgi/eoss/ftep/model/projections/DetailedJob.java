package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.api.security.FtepAccess;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.time.LocalDateTime;

/**
 * <p>Comprehensive representation of a Job entity, including outputs and jobConfig, for embedding in
 * REST responses.</p>
 */
@Projection(name = "detailedJob", types = Job.class)
public interface DetailedJob extends Identifiable<Long> {
    String getExtId();
    ShortUser getOwner();
    Job.Status getStatus();
    String getGuiUrl();
    String getStage();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    @Value("#{target.config.service.name}")
    String getServiceName();
    Multimap<String, String> getOutputs();
    JobConfig getConfig();
    @Value("#{@ftepSecurityService.getCurrentAccess(T(com.cgi.eoss.ftep.model.Job), target.id)}")
    FtepAccess getAccess();
}
