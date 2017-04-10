package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.api.security.FtepPermission;
import com.cgi.eoss.ftep.model.FtepFile;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("#{@ftepSecurityService.getCurrentPermission(target.class, target.id)}")
    FtepPermission getAccessLevel();
}
