package com.cgi.eoss.ftep.api.projections;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepFileType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * <p>Comprehensive representation of an FtepFile entity, including all catalogue metadata, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedFtepFile", types = {FtepFile.class})
public interface DetailedFtepFile extends EmbeddedId {
    URI getUri();
    UUID getRestoId();
    FtepFileType getType();
    ShortUser getOwner();
    String getFilename();
    @Value("#{com.google.common.collect.ImmutableMap.of()}")
    Map<String, Object> getMetadata();
}
