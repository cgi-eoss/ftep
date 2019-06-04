package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.security.FtepAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.net.URI;
import java.util.UUID;

/**
 * <p>Abbreviated representation of an FtepFile entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortFtepFileWorkspace", types = {FtepFile.class})
public interface ShortFtepFileWorkspace extends Identifiable<Long> {
    UUID getRestoId();
    ShortUser getOwner();
    String getFilename();
    URI getUri();
    FtepFile.Type getType();
    Long getFilesize();
    @Value("#{@ftepSecurityService.getCurrentAccess(T(com.cgi.eoss.ftep.model.FtepFile), target.id)}")
    FtepAccess getAccess();
}
