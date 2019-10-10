package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.model.SystematicProcessing;
import com.cgi.eoss.ftep.security.FtepAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Default JSON projection for embedded {@link SystematicProcessing}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortSystematicProcessing", types = {SystematicProcessing.class})
public interface ShortSystematicProcessing extends Identifiable<Long> {
    ShortUser getOwner();

    SystematicProcessing.Status getStatus();

    @Value("#{@ftepSecurityService.getCurrentAccess(T(com.cgi.eoss.ftep.model.Job), target.id)}")
    FtepAccess getAccess();
}
