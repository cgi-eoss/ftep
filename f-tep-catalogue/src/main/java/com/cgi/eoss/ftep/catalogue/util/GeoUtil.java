package com.cgi.eoss.ftep.catalogue.util;

import com.cgi.eoss.ftep.model.internal.Shapefile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.common.io.MoreFiles;
import com.vividsolutions.jts.geom.Polygon;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.Hints;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.text.WKTParser;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.filter.Filter;
import org.opengis.geometry.coordinate.LineString;
import org.opengis.geometry.coordinate.PointArray;
import org.opengis.geometry.coordinate.Position;
import org.opengis.geometry.primitive.Curve;
import org.opengis.geometry.primitive.Point;
import org.opengis.geometry.primitive.Surface;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
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

    private static final GeometryBuilder GEOMETRY_BUILDER = new GeometryBuilder(DefaultGeographicCRS.WGS84);

    private static final WKTParser WKT_PARSER = new WKTParser(GEOMETRY_BUILDER);

    private static final String DEFAULT_POINT = "POINT(0 0)";

    public static org.geojson.Point defaultPoint() {
        return wktToGeojsonPoint(DEFAULT_POINT);
    }

    public static GeoJsonObject getGeoJsonGeometry(String geometry) {
        try {
            // TODO Check for other ISO Geometry types
            return GeoUtil.wktToGeojsonPolygon(geometry);
        } catch (GeometryException e) {
            return GeoUtil.defaultPoint();
        }
    }

    public static org.geojson.Polygon wktToGeojsonPolygon(String wkt) {
        try {
            Surface surface = (Surface) WKT_PARSER.parse(wkt);
            Curve curve = (Curve) Iterables.getOnlyElement(surface.getBoundary().getExterior().getElements());
            LineString lineString = (LineString) Iterables.getOnlyElement(curve.getSegments());
            PointArray controlPoints = lineString.getControlPoints();

            List<LngLatAlt> geojsonCoords = controlPoints.stream()
                    .map(Position::getDirectPosition)
                    .map(p -> new LngLatAlt(p.getOrdinate(0), p.getOrdinate(1)))
                    .collect(Collectors.toList());

            return new org.geojson.Polygon(geojsonCoords);
        } catch (Exception e) {
            LOG.error("Could not convert WKT to GeoJson Polygon: {}", wkt, e);
            throw new GeometryException(e);
        }
    }

    public static org.geojson.Point wktToGeojsonPoint(String wkt) {
        try {
            Point point = (Point) WKT_PARSER.parse(wkt);
            return new org.geojson.Point(
                    point.getDirectPosition().getOrdinate(0),
                    point.getDirectPosition().getOrdinate(1));
        } catch (Exception e) {
            LOG.error("Could not convert WKT to GeoJson Point: {}", wkt, e);
            throw new GeometryException(e);
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
            return new GeometryJSON().read(geojsonToString(geojson)).toString();
        } catch (Exception e) {
            LOG.error("Could not convert GeoJsonObject to WKT: {}", geojson, e);
            throw new GeometryException(e);
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
            return wktToGeojsonPolygon(polygon.toString());
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
            LOG.error("Could not extract bounding box from file: {}", file);
            LOG.trace(e);
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
