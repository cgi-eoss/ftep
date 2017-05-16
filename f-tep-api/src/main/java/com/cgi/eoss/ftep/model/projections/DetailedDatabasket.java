package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.api.security.FtepPermission;
import com.cgi.eoss.ftep.model.Databasket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Set;

/**
 * <p>Comprehensive representation of a Databasket entity, including all FtepFile catalogue metadata, for embedding in
 * REST responses.</p>
 */
@Projection(name = "detailedDatabasket", types = Databasket.class)
public interface DetailedDatabasket extends EmbeddedId {
    String getName();
    String getDescription();
    ShortUser getOwner();
    @Value("#{@ftepSecurityService.getCurrentPermission(target.class, target.id)}")
    FtepPermission getAccessLevel();
    Set<ShortFtepFile> getFiles();
}
