package com.cgi.eoss.ftep.catalogue;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.internal.OutputProductMetadata;
import com.cgi.eoss.ftep.model.internal.ReferenceDataMetadata;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * <p>Centralised access to the F-TEP catalogues of reference data, output products, and external product
 * references.</p>
 */
public interface CatalogueService {
    /**
     * <p>Create a new reference data file. The storage mechanism and implementation detail depends on the {@link
     * com.cgi.eoss.ftep.catalogue.files.ReferenceDataService} in use.</p>
     * <p>This will return a persisted entity.</p>
     *
     * @param referenceData
     * @param file
     * @return
     * @throws IOException
     */
    FtepFile ingestReferenceData(ReferenceDataMetadata referenceData, MultipartFile file) throws IOException;

    /**
     * <p>Process an already-existing file, to be treated as an {@link com.cgi.eoss.ftep.model.FtepFileType#OUTPUT_PRODUCT}.</p>
     * <p>This will return a persisted entity.</p>
     *
     * @param outputProduct
     * @param path
     * @return
     * @throws IOException
     */
    FtepFile ingestOutputProduct(OutputProductMetadata outputProduct, Path path) throws IOException;

    /**
     * <p>Resolve the given {@link FtepFile} into an appropriate Spring Resource descriptor.</p>
     *
     * @param file
     * @return
     */
    Resource getAsResource(FtepFile file);

    /**
     * <p>Remove the given FtepFile from all associated external catalogues, and finally the F-TEP database itself.</p>
     *
     * @param file
     */
    void delete(FtepFile file);

}
