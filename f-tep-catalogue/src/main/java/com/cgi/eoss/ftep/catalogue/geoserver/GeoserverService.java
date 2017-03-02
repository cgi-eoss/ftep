package com.cgi.eoss.ftep.catalogue.geoserver;

import java.nio.file.Path;

/**
 * <p>Facade to a Geoserver instance, to enhance/enable F-TEP W*S functionality.</p>
 */
public interface GeoserverService {
    void ingest(String workspace, Path path, String crs);
}
