package com.cgi.eoss.ftep.catalogue.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.text.WKTParser;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.coordinate.LineString;
import org.opengis.geometry.coordinate.PointArray;
import org.opengis.geometry.coordinate.Position;
import org.opengis.geometry.primitive.Curve;
import org.opengis.geometry.primitive.Point;
import org.opengis.geometry.primitive.Surface;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Utility methods for dealing with geo-spatial data and its various java libraries.</p>
 */
@Log4j2
@UtilityClass
public class GeoUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final WKTParser WKT_PARSER = new WKTParser(new GeometryBuilder(DefaultGeographicCRS.WGS84));

    // London
    private static final String DEFAULT_POINT = "POINT(-0.1275 51.507222)";

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

}
