package com.cgi.eoss.ftep.catalogue.external;

import com.cgi.eoss.ftep.catalogue.FtepFileService;
import com.cgi.eoss.ftep.model.FtepFile;
import org.geojson.GeoJsonObject;

import java.net.URI;

public interface ExternalProductDataService extends FtepFileService {
    FtepFile ingest(GeoJsonObject geoJson);

    URI getUri(String productSource, String productId);
}
