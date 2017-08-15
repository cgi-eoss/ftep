package com.cgi.eoss.ftep.catalogue.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.text.WKTParser;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.filter.Filter;
import org.opengis.geometry.coordinate.LineString;
import org.opengis.geometry.coordinate.PointArray;
import org.opengis.geometry.coordinate.Position;
import org.opengis.geometry.primitive.Curve;
import org.opengis.geometry.primitive.Point;
import org.opengis.geometry.primitive.Surface;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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
        } catch (RuntimeException e) {
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
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
    }

    public static String geojsonToString(GeoJsonObject object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOG.error("Could not serialise GeoJsonObject: {}", object, e);
            throw new RuntimeException(e);
        }
    }

    public static GeoJsonObject stringToGeojson(String geojson) {
        try {
            return OBJECT_MAPPER.readValue(geojson, GeoJsonObject.class);
        } catch (Exception e) {
            LOG.error("Could not deserialise GeoJsonObject: {}", geojson, e);
            throw new RuntimeException(e);
        }
    }

    public static org.geojson.Polygon extractBoundingBox(Path file) {
        try {
            ReferencedEnvelope envelope = getEnvelope(file);

            Polygon polygon = JTS.toGeometry(envelope.toBounds(DefaultGeographicCRS.WGS84));
            LOG.debug("Extracted WKT bounding box from file {}: {}", file.getFileName(), polygon);
            return wktToGeojsonPolygon(polygon.toString());
        } catch (Exception e) {
            LOG.error("Could not extract bounding box from file: {}", file, e);
            throw new RuntimeException(e);
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
}
