package com.cgi.eoss.ftep.catalogue.resto;

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
}
