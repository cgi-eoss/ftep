package com.cgi.eoss.ftep.catalogue.external;

import com.cgi.eoss.ftep.catalogue.CatalogueUri;
import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>Handler for external product (e.g. S-1, S-2, Landsat) metadata and files.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class ExternalProductDataServiceImpl implements ExternalProductDataService {

    private final FtepFileDataService ftepFileDataService;
    private final RestoService resto;

    @Override
    public FtepFile ingest(GeoJsonObject geoJson) {
        // TODO Handle non-feature objects?
        Feature feature = (Feature) geoJson;

        String productId = feature.getProperty("productId");
        URI uri = CatalogueUri.valueOf(feature.getProperty("type")).build(ImmutableMap.of("productId", productId));

        return Optional.ofNullable(ftepFileDataService.getByUri(uri)).orElseGet(() -> {
            UUID restoId = resto.ingestReferenceData(feature);
            return ftepFileDataService.save(new FtepFile(uri, restoId));
        });
    }

    @Override
    public Resource resolve(FtepFile file) {
        // TODO Allow proxied access (with TEP coin cost) to some external data
        throw new UnsupportedOperationException("Direct download of external products via F-TEP is not permitted");
    }

}
