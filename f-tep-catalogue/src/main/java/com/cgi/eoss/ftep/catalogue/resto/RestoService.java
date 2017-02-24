package com.cgi.eoss.ftep.catalogue.resto;

import org.geojson.Feature;

import java.util.UUID;

/**
 * <p>Facade to a Resto instance, to enable F-TEP OpenSearch Geo/Time functionality.</p>
 */
public interface RestoService {
    /**
     * <p>Ingest the given GeoJson Feature to the Resto catalogue, and return the new record's UUID.</p>
     */
    UUID ingest(Feature feature);
}
