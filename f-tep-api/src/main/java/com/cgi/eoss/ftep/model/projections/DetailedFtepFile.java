package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.api.security.FtepPermission;
import com.cgi.eoss.ftep.model.FtepFile;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.net.URI;
import java.util.UUID;

/**
 * <p>Comprehensive representation of an FtepFile entity, including all catalogue metadata, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedFtepFile", types = FtepFile.class)
public interface DetailedFtepFile extends EmbeddedId {
    URI getUri();
    UUID getRestoId();
    FtepFile.Type getType();
    ShortUser getOwner();
    String getFilename();
    @Value("#{@ftepSecurityService.getCurrentPermission(target.class, target.id)}")
    FtepPermission getAccessLevel();
    @Value("#{@restoServiceImpl.getGeoJsonSafe(target)}")
    GeoJsonObject getMetadata();
}
