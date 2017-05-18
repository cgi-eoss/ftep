package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.api.security.FtepPermission;
import com.cgi.eoss.ftep.model.FtepService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Default JSON projection for embedded {@link FtepService}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortFtepService", types = {FtepService.class})
public interface ShortFtepService extends Identifiable<Long> {
    String getName();
    String getDescription();
    FtepService.Type getType();
    ShortUser getOwner();
    String getDockerTag();
    FtepService.Licence getLicence();
    FtepService.Status getStatus();
    @Value("#{@ftepSecurityService.isPublic(target.class, target.id)}")
    boolean isPublic();
    @Value("#{@ftepSecurityService.getCurrentPermission(target.class, target.id)}")
    FtepPermission getAccessLevel();
}
