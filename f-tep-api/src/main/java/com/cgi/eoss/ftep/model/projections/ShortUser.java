package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;

import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Abbreviated representation of a User entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortUser", types = {User.class})
public interface ShortUser extends Identifiable<Long> {
    String getName();
    String getEmail();
    String getOrganisation();
    Role getRole();
}
