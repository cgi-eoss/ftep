package com.cgi.eoss.ftep.api.projections;

import com.cgi.eoss.ftep.model.Group;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * <p>Abbreviated representation of a User entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortGroup", types = {Group.class})
public interface ShortGroup extends EmbeddedId {
    String getName();
    String getDescription();
    ShortUser getOwner();
    @Value("#{target.members.size()}")
    Integer getSize();
}
