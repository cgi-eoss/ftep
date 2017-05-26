package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.model.PublishingRequest;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Default JSON projection for embedded {@link PublishingRequest}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortPublishingRequest", types = {PublishingRequest.class})
public interface ShortPublishingRequest extends Identifiable<Long> {
    ShortUser getOwner();
    PublishingRequest.Status getStatus();
    PublishingRequest.Type getType();
    Long getAssociatedId();
}
