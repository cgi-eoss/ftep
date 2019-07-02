package com.cgi.eoss.ftep.catalogue.util;

import com.cgi.eoss.ftep.model.internal.Shapefile;
import com.cgi.eoss.ftep.model.internal.ShpFileType;
import com.cgi.eoss.ftep.model.internal.UploadableFileType;
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
import org.geotools.gml3.GMLConfiguration;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.xml.PullParser;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * <p>Utility methods for dealing with geo-spatial data and its various java libraries.</p>
 */
@Log4j2
@UtilityClass
public class GeoUtil {
    private static final String tempDirName = "tempDir";

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

    /**
     * A function to unzip files from a zipped shapefile into a temporary folder.
     * The *.shp file is returned.
     *
     * @param shapefile
     * @param tempDir
     * @return
     */
    // TODO: extract and reuse the unzip functionality from f-tep-io
    public static Path unzipShapefile(Path shapefile, Path tempDir) {
        try {
            ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(shapefile));
            byte[] buffer = new byte[1024];
            ZipEntry zipEntry;
            File shpFile = null;
            while ((zipEntry = zipIn.getNextEntry()) != null) {
                File file = new File(tempDir.toFile(), zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    file.mkdir();
                } else {
                    new File(file.getParent()).mkdirs();
                    FileOutputStream fileOut = new FileOutputStream(file);
                    int length;
                    while ((length = zipIn.read(buffer)) > 0) {
                        fileOut.write(buffer, 0, length);
                    }
                    if (zipEntry.getName().toLowerCase().endsWith(ShpFileType.SHP.extensionWithPeriod)) {
                        shpFile = file;
                    }
                    fileOut.close();
                }
            }
            zipIn.closeEntry();
            zipIn.close();
            return Optional.ofNullable(shpFile.toPath()).orElse(null);

        } catch (IOException e) {
            LOG.error("Error in unzipping the file {}:", shapefile.toUri(), e);
            throw new GeometryException(e);
        }
    }

    /**
     * Cleaning up the temporary folder containing unzipped Shapefile files
     *
     * @param tempDir
     */
    public void cleanUp(Path tempDir) {
        try {
            MoreFiles.deleteRecursively(tempDir);
        } catch (IOException e) {
            LOG.error("Failed to recursively delete the directory {}:", tempDir.toUri(), e);
            throw new GeometryException(e);
        }
    }

    /**
     * Extracting geometry from an XML file of type FIS
     *
     * @param file
     * @return
     */
    public static GeoJsonObject extractFISGeometry(Path file) {
        try {
            InputStream in = new BufferedInputStream(Files.newInputStream(file));
            GMLConfiguration gml = new GMLConfiguration();

            // TODO Model FIS data with proper XSD-generated classes
            PullParser standParser = new PullParser(gml, in, new QName("http://standardit.tapio.fi/schemas/forestData/stand/2010/08/31", "Stand"));

            List<HashMap<String, Object>> stands = new ArrayList<>();
            HashMap<String, Object> stand;
            while ((stand = (HashMap<String, Object>) standParser.parse()) != null) {
                stands.add(stand);
            }

            Polygon[] polygons = stands.stream()
                    .map(s -> (HashMap<String, HashMap<String, Object>>) s.get("StandBasicData"))
                    .map(standBasicData -> (Polygon) standBasicData.get("PolygonGeometry").get("polygonProperty"))
                    .map(polygon -> {
                        try {
                            // Convert to WGS84
                            return (Polygon) JTS.toGeographic(polygon, (CoordinateReferenceSystem) polygon.getUserData());
                        } catch (TransformException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList()).toArray(new Polygon[]{});

            com.vividsolutions.jts.geom.MultiPolygon multiPolygon = new com.vividsolutions.jts.geom.MultiPolygon(polygons, JTSFactoryFinder.getGeometryFactory());

            // FIS has a lot of small polygons in one area, so extract the bounding box (envelope)
            return getGeoJsonGeometry(multiPolygon.getEnvelope().toText());
        } catch (IOException | XMLStreamException | SAXException e) {
            LOG.error("Failed to extract geometry from the file {}:", file.toUri(), e);
            throw new GeometryException(e);
        }
    }

    /**
     * Geometry extraction controller function
     *
     * @param file
     * @param fileType
     * @param referenceDataBasedir
     * @return
     */
    public static GeoJsonObject extractGeometry(Path file, UploadableFileType fileType, Path referenceDataBasedir) {
        switch (fileType) {
            case GEOTIFF:
                return extractBoundingBox(file);
            case SHAPEFILE:
                Path tempDir = null;
                try {
                    tempDir = Files.createTempDirectory(referenceDataBasedir, tempDirName);
                    Path geometryFile = unzipShapefile(file, tempDir);
                    org.geojson.Polygon geometry = extractBoundingBox(geometryFile);
                    return geometry;
                } catch (IOException e) {
                    LOG.error("Failed to create a temporary directory:", e);
                    throw new GeometryException(e);
                } finally {
                    if (tempDir != null) {
                        cleanUp(tempDir);
                    }
                }
            case FIS:
                return extractFISGeometry(file);
            default:
                LOG.error("Illegal file type for extracting geometry: " + fileType);
                return null;
        }
    }
}
