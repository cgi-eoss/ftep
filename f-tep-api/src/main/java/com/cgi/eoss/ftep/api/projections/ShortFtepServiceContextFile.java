package com.cgi.eoss.ftep.api.projections;

import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import org.springframework.data.rest.core.config.Projection;

/**
 * <p>Default JSON projection for embedded {@link FtepServiceContextFile}s. Embeds the service as a ShortService, and
 * omits the content.</p>
 */
@Projection(name = "shortServiceFile", types = {FtepServiceContextFile.class})
public interface ShortFtepServiceContextFile extends EmbeddedId {
    ShortFtepService getService();

    String getFilename();

    boolean isExecutable();
}
