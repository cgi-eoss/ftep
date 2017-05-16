package com.cgi.eoss.ftep.catalogue.util;

import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 */
public class GeoUtilTest {

    @Test
    public void testWktToGeojsonPolygon() throws Exception {
        Polygon polygon = GeoUtil.wktToGeojsonPolygon("POLYGON((-15.029296875 57.947513433650634,10.283203125 57.947513433650634,10.283203125 40.12513173115235,-15.029296875 40.12513173115235,-15.029296875 57.947513433650634))");
        Polygon expected = new Polygon(
                new LngLatAlt(-15.029296875, 57.947513433650634),
                new LngLatAlt(10.283203125, 57.947513433650634),
                new LngLatAlt(10.283203125, 40.12513173115235),
                new LngLatAlt(-15.029296875, 40.12513173115235),
                new LngLatAlt(-15.029296875, 57.947513433650634)
        );

        assertThat(polygon, is(expected));
    }
    @Test
    public void testWktToGeojsonPoint() throws Exception {
        Point point = GeoUtil.wktToGeojsonPoint("POINT(0 0)");
        Point expected = new Point(new LngLatAlt(0, 0));

        assertThat(point, is(expected));
        assertThat(GeoUtil.defaultPoint(), is(expected));
    }

}