package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Default JSON projection for embedded {@link FtepServiceContextFile}s. Embeds the service as a ShortService, and
 * omits the content.</p>
 */
@Projection(name = "shortServiceFile", types = {FtepServiceContextFile.class})
public interface ShortFtepServiceContextFile extends Identifiable<Long> {
    ShortFtepService getService();
    String getFilename();
    boolean isExecutable();
}
