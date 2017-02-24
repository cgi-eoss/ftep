package com.cgi.eoss.ftep.catalogue.util;

import com.google.common.collect.Iterables;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
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

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Utility methods for dealing with geo-spatial data and its various java libraries.</p>
 */
@Slf4j
@UtilityClass
public class GeoUtil {
    private static final WKTParser WKT_PARSER = new WKTParser(new GeometryBuilder(DefaultGeographicCRS.WGS84));

    // London
    private static final String DEFAULT_POINT = "POINT(-0.1275 51.507222)";

    public static org.geojson.Point defaultPoint() {
        try {
            return wktToGeojsonPoint(DEFAULT_POINT);
        } catch (ParseException e) {
            // This really shouldn't ever be hit
            LOG.error("Default Point '{}' invalid!", DEFAULT_POINT, e);
            throw new RuntimeException(e);
        }
    }

    public static org.geojson.Polygon wktToGeojsonPolygon(String wkt) throws ParseException {
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
            throw e;
        }
    }

    public static org.geojson.Point wktToGeojsonPoint(String wkt) throws ParseException {
        try {
            Point point = (Point) WKT_PARSER.parse(wkt);
            return new org.geojson.Point(
                    point.getDirectPosition().getOrdinate(0),
                    point.getDirectPosition().getOrdinate(1));
        } catch (Exception e) {
            LOG.error("Could not convert WKT to GeoJson Point: {}", wkt, e);
            throw e;
        }
    }

}
