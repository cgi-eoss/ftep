package com.cgi.eoss.ftep.api.projections;

import com.cgi.eoss.ftep.model.User;
import org.springframework.data.rest.core.config.Projection;

/**
 * <p>Abbreviated representation of a User entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortUser", types = {User.class})
public interface ShortUser extends EmbeddedId {
    String getName();
}
