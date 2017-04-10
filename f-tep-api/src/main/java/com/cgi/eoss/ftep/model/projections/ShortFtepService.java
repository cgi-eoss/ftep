package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.api.security.FtepPermission;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.ServiceLicence;
import com.cgi.eoss.ftep.model.ServiceStatus;
import com.cgi.eoss.ftep.model.ServiceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * <p>Default JSON projection for embedded {@link FtepService}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortFtepService", types = {FtepService.class})
public interface ShortFtepService extends EmbeddedId {
    String getName();
    String getDescription();
    ServiceType getType();
    ShortUser getOwner();
    String getDockerTag();
    ServiceLicence getLicence();
    ServiceStatus getStatus();
    @Value("#{@ftepSecurityService.isPublic(target.class, target.id)}")
    boolean isPublic();
    @Value("#{@ftepSecurityService.getCurrentPermission(target.class, target.id)}")
    FtepPermission getAccessLevel();
}
