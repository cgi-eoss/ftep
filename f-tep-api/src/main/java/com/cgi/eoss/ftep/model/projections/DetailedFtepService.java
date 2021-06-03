package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.security.FtepAccess;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Comprehensive representation of an FtepService entity, including the full description of input and output fields, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedFtepService", types = FtepService.class)
public interface DetailedFtepService extends Identifiable<Long> {

    String getName();
    String getDescription();
    ShortUser getOwner();
    FtepService.Type getType();
    String getDockerTag();
    FtepService.Licence getLicence();
    FtepService.Status getStatus();
    String getApplicationPort();
    FtepServiceDescriptor getServiceDescriptor();
    FtepServiceDescriptor getEasyModeServiceDescriptor();
    String getEasyModeParameterTemplate();
    @Value("#{@ftepSecurityService.getCurrentAccess(T(com.cgi.eoss.ftep.model.FtepService), target.id)}")
    FtepAccess getAccess();
    Boolean getMountEodata();

}
