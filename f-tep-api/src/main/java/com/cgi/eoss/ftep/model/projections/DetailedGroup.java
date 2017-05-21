package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.api.security.FtepPermission;
import com.cgi.eoss.ftep.model.Group;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.util.Set;

/**
 * <p>Comprehensive representation of a Group entity for embedding in REST responses.</p>
 */
@Projection(name = "detailedGroup", types = {Group.class})
public interface DetailedGroup extends Identifiable<Long> {
    String getName();
    String getDescription();
    ShortUser getOwner();
    @Value("#{@ftepSecurityService.getCurrentPermission(target.class, target.id)}")
    FtepPermission getAccessLevel();
    Set<ShortUser> getMembers();
}
