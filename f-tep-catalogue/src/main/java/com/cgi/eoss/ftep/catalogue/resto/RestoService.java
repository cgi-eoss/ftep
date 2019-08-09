package com.cgi.eoss.ftep.catalogue.resto;

import com.cgi.eoss.ftep.model.FtepFile;
import org.geojson.GeoJsonObject;

import java.util.UUID;

/**
 * <p>Facade to a Resto instance, to enable F-TEP OpenSearch Geo/Time functionality.</p>
 */
public interface RestoService {
    /**
     * <p>Ingest the given GeoJsonObject to the Resto catalogue, in the F-TEP Reference Data collection, and return the
     * new record's UUID.</p>
     */
    UUID ingestReferenceData(GeoJsonObject object);

    /**
     * <p>Ingest the given GeoJsonObject to the Resto catalogue, in the F-TEP Output Project collection, and return the
     * new record's UUID.</p>
     */
    UUID ingestOutputProduct(GeoJsonObject object);

    /**
     * <p>Ingest the given GeoJsonObject to the Resto catalogue, in the given collection, and return the new record's
     * UUID.</p>
     */
    UUID ingestExternalProduct(String collection, GeoJsonObject object);

    /**
     * <p>Remove the given F-TEP Reference Data product from the Resto catalogue.</p>
     */
    void deleteReferenceData(UUID restoId);

    /**
     * <p>Remove the given F-TEP Output Data product from the Resto catalogue.</p>
     */
    void deleteOutputData(UUID restoId);

    /**
     * <p>Remove the given F-TEP External Data product from the Resto catalogue</p>
     */
    void deleteExternalData(UUID restoId);

    /**
     * @return The Resto catalogue GeoJSON data for the given FtepFile.
     */
    GeoJsonObject getGeoJson(FtepFile ftepFile);

    /**
     * @return The Resto catalogue GeoJSON data for the given FtepFile, or null if any exception is encountered.
     */
    GeoJsonObject getGeoJsonSafe(FtepFile ftepFile);

    /**
     * @return The Resto collection name identifying F-TEP reference data.
     */
    String getReferenceDataCollection();

    /**
     * @return The Resto collection name identifying F-TEP output products.
     */
    String getOutputProductsCollection();
}
