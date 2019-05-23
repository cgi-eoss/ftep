package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.api.controllers.AclsApi;
import com.cgi.eoss.ftep.catalogue.util.GeoUtil;
import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.security.FtepAccess;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * <p>Comprehensive representation of an FtepFile entity, including all catalogue metadata, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedFtepFileWorkspace", types = FtepFile.class)
public interface DetailedFtepFileWorkspace extends Identifiable<Long> {
    URI getUri();
    UUID getRestoId();
    FtepFile.Type getType();
    ShortUser getOwner();
    String getFilename();
    Long getFilesize();
    @Value("#{T(com.cgi.eoss.ftep.catalogue.util.GeoUtil).geojsonToWkt(@restoServiceImpl.getGeoJsonSafe(target))}")
    String getGeometry();
    @Value("#{@databasketsApi.findByFilesIsContaining(target)}")
    List<ShortDatabasket> getContainedInDatabaskets();
    @Value("#{@aclsApi.getFtepFileAcls(target.id)}")
    AclsApi.FtepAccessControlList getSharedWithGroups();
    @Value("#{@ftepSecurityService.getCurrentAccess(T(com.cgi.eoss.ftep.model.FtepFile), target.id)}")
    FtepAccess getAccess();
}
