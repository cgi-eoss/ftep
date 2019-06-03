package com.cgi.eoss.ftep.catalogue.util;

import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GeoUtilTest {

    private Path shapefileDurango;
    private Path shapefileChiapas;
    private Path geotiffDurango;

    @Before
    public void setUp() throws URISyntaxException {
        shapefileDurango = Paths.get(getClass().getResource("/DurangoRef/F-TEPsampleDurangoLandCoverRefPolygons.shp").toURI());
        shapefileChiapas = Paths.get(getClass().getResource("/F-TEPchiapasLandCover/F-TEPchiapasLandCover.shp").toURI());
        geotiffDurango = Paths.get(getClass().getResource("/subset_0_of_S2A_OPER_MTD_SAFL1C_PDMC_20160521T232007_R055_V20160521T174723_20160521T174723_resampled_RGB.tif").toURI());
    }

    @Test
    public void testWktToGeojsonPolygon() throws Exception {
        Polygon polygon = (Polygon) GeoUtil.getGeoJsonGeometry("POLYGON ((-15.029296875 57.947513433650634, 10.283203125 57.947513433650634, 10.283203125 40.12513173115235, -15.029296875 40.12513173115235, -15.029296875 57.947513433650634))");
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
    public void testGeojsonToWktPolygon() {
        String wktPolygon = GeoUtil.geojsonToWkt(new Polygon(
                new LngLatAlt(-15.029296875, 57.947513433650634),
                new LngLatAlt(10.283203125, 57.947513433650634),
                new LngLatAlt(10.283203125, 40.12513173115235),
                new LngLatAlt(-15.029296875, 40.12513173115235),
                new LngLatAlt(-15.029296875, 57.947513433650634)
        ));
        assertThat(wktPolygon, is("POLYGON ((-15.029296875 57.947513433650634, 10.283203125 57.947513433650634, 10.283203125 40.12513173115235, -15.029296875 40.12513173115235, -15.029296875 57.947513433650634))"));

        String wktPoint = GeoUtil.geojsonToWkt(new Point(new LngLatAlt(0, 0)));
        assertThat(wktPoint, is("POINT (0 0)"));
    }

    @Test
    public void testGeojsonToWktPoint() {
        String wktPoint = GeoUtil.geojsonToWkt(new Point(new LngLatAlt(0, 0)));
        assertThat(wktPoint, is("POINT (0 0)"));
    }

    @Test
    public void testGeojsonToWktFeature() {
        String wktPolygon = GeoUtil.geojsonToWkt(GeoUtil.stringToGeojson(EXAMPLE_FEATURE_JSON));
        assertThat(wktPolygon, is("POINT (0 0)"));
    }

    @Test
    public void testWktToGeojsonPoint() throws Exception {
        Point point = (Point) GeoUtil.getGeoJsonGeometry("POINT(0 0)");
        Point expected = new Point(new LngLatAlt(0, 0));

        assertThat(point, is(expected));
        assertThat(GeoUtil.defaultPoint(), is(expected));
    }

    @Test
    public void extractBoundingBoxShapefileChiapas() throws Exception {
        Polygon polygon = GeoUtil.extractBoundingBox(shapefileChiapas);
        Polygon expected = new Polygon(
                new LngLatAlt(-92.91927146994584, 15.779673670302039),
                new LngLatAlt(-92.30984753765863, 15.779673670302039),
                new LngLatAlt(-92.30984753765863, 16.255487070951077),
                new LngLatAlt(-92.91927146994584, 16.255487070951077),
                new LngLatAlt(-92.91927146994584, 15.779673670302039)
        );

        assertThat(polygon, is(expected));
    }

    @Test
    public void extractBoundingBoxShapefileDurango() throws Exception {
        Polygon polygon = GeoUtil.extractBoundingBox(shapefileDurango);
        Polygon expected = new Polygon(
                new LngLatAlt(-104.88452021695508, 23.547911691931287),
                new LngLatAlt(-104.03747413959455, 23.547911691931287),
                new LngLatAlt(-104.03747413959455, 24.390684514620556),
                new LngLatAlt(-104.88452021695508, 24.390684514620556),
                new LngLatAlt(-104.88452021695508, 23.547911691931287)
        );

        assertThat(polygon, is(expected));
    }

    @Test
    public void extractBoundingBoxGeotiff() throws Exception {
        Polygon polygon = GeoUtil.extractBoundingBox(geotiffDurango);
        Polygon expected = new Polygon(
                new LngLatAlt(-106.9893803701548, 23.945607806036303),
                new LngLatAlt(-106.97919638534522, 23.945607806036303),
                new LngLatAlt(-106.97919638534522, 23.95496731829442),
                new LngLatAlt(-106.9893803701548, 23.95496731829442),
                new LngLatAlt(-106.9893803701548, 23.945607806036303)
        );

        assertThat(polygon, is(expected));
    }

    @Test
    public void extractEpsgShapefileChiapas() throws Exception {
        String epsg = GeoUtil.extractEpsg(shapefileChiapas);
        assertThat(epsg, is("EPSG:32615"));
    }

    @Test
    public void extractEpsgShapefileDurango() throws Exception {
        String epsg = GeoUtil.extractEpsg(shapefileDurango);
        assertThat(epsg, is("EPSG:4326"));
    }

    @Test
    public void extractEpsgGeotiff() throws Exception {
        String epsg = GeoUtil.extractEpsg(geotiffDurango);
        assertThat(epsg, is("EPSG:32612"));
    }

    private static final String EXAMPLE_FEATURE_JSON = "{\n" +
            "  \"type\": \"Feature\",\n" +
            "  \"properties\": {\n" +
            "    \"collection\": \"ftepRefData\",\n" +
            "    \"productIdentifier\": \"testfile.txt\"\n" +
            "  },\n" +
            "  \"geometry\": {\n" +
            "    \"type\": \"Point\",\n" +
            "    \"coordinates\": [\n" +
            "      0,\n" +
            "      0\n" +
            "    ]\n" +
            "  },\n" +
            "  \"id\": \"18814a8a-69cf-5ef6-b13d-da0d338abd56\"\n" +
            "}";

}
