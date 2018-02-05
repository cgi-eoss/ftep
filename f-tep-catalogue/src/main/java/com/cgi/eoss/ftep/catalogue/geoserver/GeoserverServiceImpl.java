package com.cgi.eoss.ftep.catalogue.geoserver;

import com.cgi.eoss.ftep.catalogue.IngestionException;
import com.google.common.base.Preconditions;
import com.google.common.io.MoreFiles;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;

import static it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy.REPROJECT_TO_DECLARED;

@Component
@Log4j2
public class GeoserverServiceImpl implements GeoserverService {

    private static final String RASTER_STYLE = "raster";

    @Getter
    private final HttpUrl externalUrl;
    private final GeoServerRESTPublisher publisher;
    private final GeoServerRESTReader reader;

    @Value("${ftep.catalogue.geoserver.enabled:true}")
    private boolean geoserverEnabled;

    @Value("#{'${ftep.catalogue.geoserver.ingest-filetypes:TIF}'.split(',')}")
    private Set<String> ingestableFiletypes;

    @Autowired
    public GeoserverServiceImpl(@Value("${ftep.catalogue.geoserver.url:http://ftep-geoserver:9080/geoserver/}") String url,
                                @Value("${ftep.catalogue.geoserver.externalUrl:http://ftep-geoserver:9080/geoserver/}") String externalUrl,
                                @Value("${ftep.catalogue.geoserver.username:ftepgeoserver}") String username,
                                @Value("${ftep.catalogue.geoserver.password:ftepgeoserverpass}") String password) throws MalformedURLException {
        this.externalUrl = HttpUrl.parse(externalUrl);
        GeoServerRESTManager geoserver = new GeoServerRESTManager(new URL(url), username, password);
        this.publisher = geoserver.getPublisher();
        this.reader = geoserver.getReader();
    }

    @Override
    public String ingest(String workspace, Path path, String crs) {
        Path fileName = path.getFileName();
        String datastoreName = MoreFiles.getNameWithoutExtension(fileName);
        String layerName = MoreFiles.getNameWithoutExtension(fileName);

        if (!geoserverEnabled) {
            LOG.warn("Geoserver is disabled; 'ingested' file: {}:{}", workspace, layerName);
            return null;
        }

        ensureWorkspaceExists(workspace);

        if (!isIngestibleFile(fileName.toString())) {
            // TODO Ingest more filetypes
            LOG.info("Unable to ingest product with filename: {}" + fileName);
            return null;
        }

        try {
            RESTCoverageStore restCoverageStore = publishExternalGeoTIFF(workspace, datastoreName, path.toFile(), layerName, crs, REPROJECT_TO_DECLARED, RASTER_STYLE);
            LOG.info("Ingested GeoTIFF to geoserver with id: {}:{}", workspace, layerName);
            return restCoverageStore.getURL();
        } catch (FileNotFoundException e) {
            LOG.error("Geoserver was unable to publish file: {}", path, e);
            throw new IngestionException(e);
        }
    }

    @Override
    public boolean isIngestibleFile(String filename) {
        return ingestableFiletypes.stream().anyMatch(ft -> filename.toUpperCase().endsWith("." + ft.toUpperCase()));
    }

    @Override
    public void delete(String workspace, String layerName) {
        if (!geoserverEnabled) {
            LOG.warn("Geoserver is disabled; no deletion occurring for {}:{}", workspace, layerName);
            return;
        }

        publisher.removeLayer(workspace, layerName);
        LOG.info("Deleted layer from geoserver: {}{}", workspace, layerName);
    }

    private void ensureWorkspaceExists(String workspace) {
        if (!reader.existsWorkspace(workspace)) {
            LOG.info("Creating new workspace {}", workspace);
            publisher.createWorkspace(workspace);
        }
    }

    private RESTCoverageStore publishExternalGeoTIFF(String workspace, String storeName, File geotiff,
                                                     String coverageName, String srs, GSResourceEncoder.ProjectionPolicy policy, String defaultStyle)
            throws FileNotFoundException {
        for (Object param : new Object[]{workspace, storeName, geotiff, coverageName, srs, policy, defaultStyle}){
            Preconditions.checkNotNull(param, "Unable to publish GeoTIFF with null parameter");
        }

        // config coverage props (srs)
        final GSCoverageEncoder coverageEncoder = new GSCoverageEncoder();
        coverageEncoder.setName(coverageName);
        coverageEncoder.setTitle(coverageName);
        coverageEncoder.setSRS(srs);
        coverageEncoder.setProjectionPolicy(policy);

        // config layer props (style, ...)
        final GSLayerEncoder layerEncoder = new GSLayerEncoder();
        layerEncoder.setDefaultStyle(defaultStyle);

        return publisher.publishExternalGeoTIFF(workspace, storeName, geotiff, coverageEncoder, layerEncoder);
    }

}
