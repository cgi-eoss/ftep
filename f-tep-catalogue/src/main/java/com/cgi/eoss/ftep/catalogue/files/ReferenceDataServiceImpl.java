package com.cgi.eoss.ftep.catalogue.files;

import com.cgi.eoss.ftep.catalogue.CatalogueUri;
import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.catalogue.util.GeoUtil;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.text.WKTParser;
import org.opengis.geometry.Geometry;
import org.opengis.geometry.primitive.Point;
import org.opengis.geometry.primitive.Primitive;
import org.opengis.geometry.primitive.Surface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.Collection;
import java.util.UUID;

@Slf4j
@Component
public class ReferenceDataServiceImpl implements ReferenceDataService {

    private final Path referenceDataBasedir;
    private final GeometryBuilder geometryBuilder;
    private final WKTParser wktParser;
    private final RestoService resto;

    @Autowired
    public ReferenceDataServiceImpl(@Qualifier("referenceDataBasedir") Path referenceDataBasedir, GeometryBuilder geometryBuilder, WKTParser wktParser, RestoService resto) {
        this.referenceDataBasedir = referenceDataBasedir;
        this.geometryBuilder = geometryBuilder;
        this.wktParser = wktParser;
        this.resto = resto;
    }

    @Override
    public FtepFile store(User owner, String filename, String geometry, MultipartFile multipartFile) throws IOException {
        Path dest = referenceDataBasedir.resolve(String.valueOf(owner.getId())).resolve(filename);
        LOG.info("Saving new reference data to: {}", dest);

        if (Files.exists(dest)) {
            LOG.warn("Found already-existing reference data, overwriting: {}", dest);
        }

        Files.copy(multipartFile.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        Feature feature = new Feature();
        feature.setId(owner.getName() + "_" + filename);
        feature.setGeometry(getGeoJsonGeometry(geometry));
        // TODO Determine properties for refdata
        feature.setProperties(ImmutableMap.of());

        UUID restoId = resto.ingest(feature);
        URI uri = CatalogueUri.REFERENCE_DATA.build(
                ImmutableMap.of(
                        "owner", owner.getName(),
                        "uuid", restoId.toString()));

        FtepFile ftepFile = new FtepFile(uri, restoId);
        ftepFile.setOwner(owner);
        ftepFile.setFilename(filename);
        return ftepFile;
    }

    private GeoJsonObject getGeoJsonGeometry(String geometry) {
        // TODO Check for other ISO Geometry types
        try {
            return GeoUtil.wktToGeojsonPolygon(geometry);
        } catch (ParseException e) {
            return GeoUtil.defaultPoint();
        }
    }

}
