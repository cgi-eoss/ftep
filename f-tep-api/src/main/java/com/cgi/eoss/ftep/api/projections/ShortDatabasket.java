package com.cgi.eoss.ftep.api.projections;

import com.cgi.eoss.ftep.model.Databasket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * <p>Abbreviated representation of a Databasket entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortDatabasket", types = {Databasket.class})
public interface ShortDatabasket extends EmbeddedId {
    String getName();
    String getDescription();
    ShortUser getOwner();
    @Value("#{target.files.size()}")
    Integer getSize();
}
