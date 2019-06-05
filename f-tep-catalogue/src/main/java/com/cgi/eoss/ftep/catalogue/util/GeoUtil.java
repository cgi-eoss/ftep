package com.cgi.eoss.ftep.catalogue.util;

import com.cgi.eoss.ftep.model.internal.Shapefile;
import com.cgi.eoss.ftep.model.internal.ShpFileType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.MoreFiles;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.Point;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.Hints;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * <p>Utility methods for dealing with geo-spatial data and its various java libraries.</p>
 */
@Log4j2
@UtilityClass
public class GeoUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final WKTReader2 WKT_READER = new WKTReader2(JTSFactoryFinder.getGeometryFactory());

    private static final String DEFAULT_POINT_WKT = "POINT(0 0)";

    private static final org.geojson.Point DEFAULT_POINT = (org.geojson.Point) getGeoJsonGeometry(DEFAULT_POINT_WKT);

    public static Point defaultPoint() {
        return DEFAULT_POINT;
    }

    public static GeoJsonObject getGeoJsonGeometry(String wkt) {
        try {
            Geometry geometry = WKT_READER.read(wkt);
            return stringToGeojson(new GeometryJSON(15).toString(geometry));
        } catch (GeometryException | ParseException e) {
            return DEFAULT_POINT;
        }
    }

    public static String geojsonToString(GeoJsonObject object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOG.error("Could not serialise GeoJsonObject: {}", object, e);
            throw new GeometryException(e);
        }
    }

    public static String geojsonToWkt(GeoJsonObject geojson) {
        try {
            GeoJsonObject geoJsonGeometry;

            // The geojson lib doesn't offer on the interface geometry...
            if (geojson instanceof Feature) {
                geoJsonGeometry = ((Feature) geojson).getGeometry();
            } else {
                geoJsonGeometry = geojson;
            }

            return new GeometryJSON().read(geojsonToString(geoJsonGeometry)).toString();
        } catch (Exception e) {
            LOG.error("Could not convert GeoJsonObject to WKT: {}", geojson, e);
            throw new GeometryException(e);
        }
    }

    public static String geojsonToWktSafe(GeoJsonObject geojson) {
        try {
            return Optional.ofNullable(geojson).map(GeoUtil::geojsonToWkt).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static GeoJsonObject stringToGeojson(String geojson) {
        try {
            return OBJECT_MAPPER.readValue(geojson, GeoJsonObject.class);
        } catch (Exception e) {
            LOG.error("Could not deserialise GeoJsonObject: {}", geojson, e);
            throw new GeometryException(e);
        }
    }

    public static org.geojson.Polygon extractBoundingBox(Path file) {
        try {
            ReferencedEnvelope envelope = getEnvelope(file);

            Polygon polygon = JTS.toGeometry(envelope.toBounds(DefaultGeographicCRS.WGS84));
            LOG.debug("Extracted WKT bounding box from file {}: {}", file.getFileName(), polygon);
            return (org.geojson.Polygon) getGeoJsonGeometry(polygon.toString());
        } catch (Exception e) {
            LOG.error("Could not extract bounding box from file: {}", file);
            LOG.trace(e);
            throw new GeometryException(e);
        }
    }

    public static String extractEpsg(Path file) {
        try {
            CoordinateReferenceSystem crs = getCrs(file);

            Integer epsgCode = CRS.lookupEpsgCode(crs, true);
            String epsg = "EPSG:" + epsgCode;

            LOG.debug("Extracted EPSG from file {}: {}", file.getFileName(), epsg);
            return epsg;
        } catch (Exception e) {
            throw new GeometryException(e);
        }
    }

    private static ReferencedEnvelope getEnvelope(Path file) throws IOException {
        // General metadata
        DataStore dataStore = DataStoreFinder.getDataStore(ImmutableMap.of("url", file.toUri().toURL()));
        if (dataStore != null) {
            SimpleFeatureCollection featureCollection = dataStore.getFeatureSource(dataStore.getTypeNames()[0]).getFeatures(Filter.INCLUDE);
            return featureCollection.getBounds();
        }

        // Raster data
        AbstractGridFormat gridFormat = GridFormatFinder.findFormat(file.toUri().toURL());
        if (gridFormat != null && !(gridFormat instanceof UnknownFormat)) {
            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            AbstractGridCoverage2DReader reader = gridFormat.getReader(file.toUri().toURL(), hints);
            return ReferencedEnvelope.reference(reader.getOriginalEnvelope());
        }

        throw new UnsupportedOperationException("Could not extract geometry envelope from " + file.toString());
    }

    private static CoordinateReferenceSystem getCrs(Path file) throws IOException {
        // General metadata
        DataStore dataStore = DataStoreFinder.getDataStore(ImmutableMap.of("url", file.toUri().toURL()));
        if (dataStore != null) {
            SimpleFeatureCollection featureCollection = dataStore.getFeatureSource(dataStore.getTypeNames()[0]).getFeatures(Filter.INCLUDE);
            return featureCollection.getSchema().getCoordinateReferenceSystem();
        }

        // Raster data
        AbstractGridFormat gridFormat = GridFormatFinder.findFormat(file.toUri().toURL());
        if (gridFormat != null && !(gridFormat instanceof UnknownFormat)) {
            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            AbstractGridCoverage2DReader reader = gridFormat.getReader(file.toUri().toURL(), hints);
            return reader.getCoordinateReferenceSystem();
        }

        throw new UnsupportedOperationException("Could not extract CRS from " + file.toString());
    }

    public static Shapefile zipShapeFile(Path shapefile, boolean deleteContentsAfterZipping) throws IOException {
        String shapefileNameBase = MoreFiles.getNameWithoutExtension(shapefile);
        Set<Path> shapefileComponents = Arrays.stream(ShpFileType.values())
                .map(type -> shapefile.resolveSibling(shapefileNameBase + type.extensionWithPeriod))
                .filter(Files::exists)
                .collect(Collectors.toSet());

        Path shapefileZip = shapefile.resolveSibling(shapefileNameBase + ".zip");


        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(shapefileZip))) {
            for (Path shapefileComponent : shapefileComponents) {
                ZipEntry zipEntry = new ZipEntry(shapefileComponent.getFileName().toString());
                zipOut.putNextEntry(zipEntry);
                try (InputStream fis = Files.newInputStream(shapefileComponent)) {
                    ByteStreams.copy(fis, zipOut);
                }

                if (deleteContentsAfterZipping) {
                    Files.delete(shapefileComponent);
                }
            }
        }

        return Shapefile.builder()
                .zip(shapefileZip)
                .contents(shapefileComponents)
                .build();
    }
}
