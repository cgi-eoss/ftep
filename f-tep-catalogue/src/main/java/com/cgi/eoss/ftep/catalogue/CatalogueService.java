package com.cgi.eoss.ftep.catalogue;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * <p>Centralised access to the F-TEP catalogues of reference data, output products, and external product
 * references.</p>
 */
public interface CatalogueService {
    /**
     * <p>Create a new reference data file. The storage mechanism and implementation detail depends on the {@link
     * com.cgi.eoss.ftep.catalogue.files.ReferenceDataService} in use.</p>
     *
     * @param user
     * @param filename
     * @param geometry
     * @param file
     * @return
     * @throws IOException
     */
    FtepFile createReferenceData(User user, String filename, String geometry, MultipartFile file) throws IOException;

    /**
     * <p>Resolve the given {@link FtepFile} into an appropriate Spring Resource descriptor.</p>
     * @param file
     * @return
     */
    Resource getAsResource(FtepFile file);
}
