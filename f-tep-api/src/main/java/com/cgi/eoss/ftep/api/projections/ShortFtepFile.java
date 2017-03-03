package com.cgi.eoss.ftep.api.projections;

import com.cgi.eoss.ftep.model.FtepFile;
import org.springframework.data.rest.core.config.Projection;

import java.util.UUID;

/**
 * <p>Abbreviated representation of an FtepFile entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortFtepFile", types = {FtepFile.class})
public interface ShortFtepFile extends EmbeddedId {
    UUID getRestoId();
    ShortUser getOwner();
    String getFilename();
}
