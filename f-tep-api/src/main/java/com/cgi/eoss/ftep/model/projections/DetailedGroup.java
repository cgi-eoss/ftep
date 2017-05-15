package com.cgi.eoss.ftep.model.projections;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import com.cgi.eoss.ftep.api.security.FtepPermission;
import com.cgi.eoss.ftep.model.Group;

/**
 * <p>Comprehensive representation of a Group entity for embedding in REST responses.</p>
 */
@Projection(name = "detailedGroup", types = {Group.class})
public interface DetailedGroup extends EmbeddedId {
    String getName();
    String getDescription();
    ShortUser getOwner();
    @Value("#{@ftepSecurityService.getCurrentPermission(target.class, target.id)}")
    FtepPermission getAccessLevel();
    Set<ShortUser> getMembers();
}
