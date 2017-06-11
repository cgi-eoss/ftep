package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.security.FtepAccess;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.util.Set;

/**
 * <p>Comprehensive representation of a Project entity for embedding in REST responses.</p>
 */
@Projection(name = "detailedProject", types = {Project.class})
public interface DetailedProject extends Identifiable<Long> {
    String getName();
    String getDescription();
    ShortUser getOwner();
    Set<ShortDatabasket> getDatabaskets();
    Set<ShortFtepService> getServices();
    Set<JobConfig> getJobConfigs();
    @Value("#{@ftepSecurityService.getCurrentAccess(T(com.cgi.eoss.ftep.model.Project), target.id)}")
    FtepAccess getAccess();
}
