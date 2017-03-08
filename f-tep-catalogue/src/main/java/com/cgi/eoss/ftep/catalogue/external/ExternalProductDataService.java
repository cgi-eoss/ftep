package com.cgi.eoss.ftep.catalogue.external;

import com.cgi.eoss.ftep.catalogue.FtepFileService;
import com.cgi.eoss.ftep.model.FtepFile;
import org.geojson.GeoJsonObject;

public interface ExternalProductDataService extends FtepFileService {
    FtepFile ingest(GeoJsonObject geoJson);
}
