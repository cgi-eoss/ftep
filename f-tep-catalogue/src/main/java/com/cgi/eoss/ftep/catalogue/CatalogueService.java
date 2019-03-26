package com.cgi.eoss.ftep.catalogue;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.internal.OutputFileMetadata;
import com.cgi.eoss.ftep.model.internal.OutputProductMetadata;
import com.cgi.eoss.ftep.model.internal.ReferenceDataMetadata;
import okhttp3.HttpUrl;
import org.geojson.GeoJsonObject;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

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
     * <p>Create a path reference suitable for creating a new output product file.</p>
     * <p>This may be used as a "thin provisioning" method, e.g. to gain access to an output stream to write new output
     * file contents.</p>
     *
     * @param outputProduct
     * @param filename
     * @return
     */
    Path provisionNewOutputProduct(OutputProductMetadata outputProduct, String filename) throws IOException;

    /**
     * <p>Process an already-existing file, to be treated as an {@link FtepFile.Type#OUTPUT_PRODUCT}.</p>
     * <p>This will return a persisted entity.</p>
     *
     * @param outputFileMetadata
     * @param path
     * @return
     * @throws IOException
     */
    FtepFile ingestOutputProduct(OutputFileMetadata outputFileMetadata, Path path) throws IOException;

    /**
     * <p>Store an external product's metadata for later reference by F-TEP.</p>
     *
     * @param geoJson
     * @return
     */
    FtepFile indexExternalProduct(GeoJsonObject geoJson);

    /**
     * <p>Resolve the given {@link FtepFile} into an appropriate Spring Resource descriptor.</p>
     *
     * @param file
     * @return
     */
    Resource getAsResource(FtepFile file);

    /**
     * <p>Resolve the given {@link FtepFile}s into a zipping Spring Resource.</p>
     *
     *
     * @param filename
     * @param files
     * @return
     */
    Resource getAsZipResource(String filename, Collection<FtepFile> files);

    /**
     * <p>Remove the given FtepFile from all associated external catalogues, and finally the F-TEP database itself.</p>
     *
     * @param file
     */
    void delete(FtepFile file) throws IOException;

    /**
     * <p>Generate an appropriate WMS URL for the given file.</p>
     *
     * @param type
     * @param uri
     * @return
     */
    HttpUrl getWmsUrl(FtepFile.Type type, URI uri);

    /**
     * <p>Determine whether the given user has read access to the object represented by the given URI ({@link
     * com.cgi.eoss.ftep.model.Databasket} or {@link FtepFile}).</p>
     * <p>This operation is recursive; access to a Databasket is granted only if all contents of the Databasket are also
     * readable.</p>
     *
     * @param user
     * @param uri
     * @return
     */
    boolean canUserRead(User user, URI uri);

    /**
     * @return The FtepFile representation of the given URI, or empty if no such FtepFile exists.
     */
    Optional<FtepFile> get(URI uri);

    /**
     * @return A Stream representing the products identified by the given URI - either the URI itself, or the expanded
     * database contents.
     */
    Stream<URI> expand(URI uri);
}
