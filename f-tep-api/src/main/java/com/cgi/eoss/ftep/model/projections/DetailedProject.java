package com.cgi.eoss.ftep.model.projections;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import com.cgi.eoss.ftep.api.security.FtepPermission;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Project;

/**
 * <p>Comprehensive representation of a Project entity for embedding in REST responses.</p>
 */
@Projection(name = "detailedProject", types = {Project.class})
public interface DetailedProject extends EmbeddedId {
    String getName();
    String getDescription();
    ShortUser getOwner();
    Set<ShortDatabasket> getDatabaskets();
    Set<ShortFtepService> getServices();
    Set<JobConfig> getJobConfigs();
    @Value("#{@ftepSecurityService.getCurrentPermission(target.class, target.id)}")
    FtepPermission getAccessLevel();
}
