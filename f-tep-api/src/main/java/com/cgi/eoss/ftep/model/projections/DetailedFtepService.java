package com.cgi.eoss.ftep.model.projections;

import org.springframework.data.rest.core.config.Projection;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.model.ServiceLicence;
import com.cgi.eoss.ftep.model.ServiceStatus;
import com.cgi.eoss.ftep.model.ServiceType;

/**
 * <p>Comprehensive representation of an FtepService entity, including the full description of input and output fields, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedFtepService", types = FtepService.class)
public interface DetailedFtepService extends EmbeddedId {

    String getName();
    String getDescription();
    ShortUser getOwner();
    ServiceType getType();
    String getDockerTag();
    ServiceLicence getLicence();
    ServiceStatus getStatus();
    FtepServiceDescriptor getServiceDescriptor();

}
