package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.api.security.FtepAccess;
import com.cgi.eoss.ftep.model.Databasket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.util.Set;

/**
 * <p>Comprehensive representation of a Databasket entity, including all FtepFile catalogue metadata, for embedding in
 * REST responses.</p>
 */
@Projection(name = "detailedDatabasket", types = Databasket.class)
public interface DetailedDatabasket extends Identifiable<Long> {
    String getName();
    String getDescription();
    ShortUser getOwner();
    Set<ShortFtepFile> getFiles();
    @Value("#{@ftepSecurityService.getCurrentAccess(target.class, target.id)}")
    FtepAccess getAccess();
}
