package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.security.FtepAccess;
import com.cgi.eoss.ftep.model.FtepFile;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.net.URI;
import java.util.UUID;

/**
 * <p>Comprehensive representation of an FtepFile entity, including all catalogue metadata, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedFtepFile", types = FtepFile.class)
public interface DetailedFtepFile extends Identifiable<Long> {
    URI getUri();
    UUID getRestoId();
    FtepFile.Type getType();
    ShortUser getOwner();
    String getFilename();
    @Value("#{@restoServiceImpl.getGeoJsonSafe(target)}")
    GeoJsonObject getMetadata();
    @Value("#{@ftepSecurityService.getCurrentAccess(T(com.cgi.eoss.ftep.model.FtepFile), target.id)}")
    FtepAccess getAccess();
}
