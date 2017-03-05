package com.cgi.eoss.ftep.catalogue.external;

import com.cgi.eoss.ftep.model.FtepFile;
import org.geojson.GeoJsonObject;
import org.springframework.core.io.Resource;

public interface ExternalProductDataService {
    FtepFile ingest(GeoJsonObject geoJson);
    Resource resolve(FtepFile file);
}
