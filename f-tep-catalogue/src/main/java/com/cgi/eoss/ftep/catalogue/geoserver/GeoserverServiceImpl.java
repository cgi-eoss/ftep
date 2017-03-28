package com.cgi.eoss.ftep.catalogue.geoserver;

import com.google.common.io.MoreFiles;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import static it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy.REPROJECT_TO_DECLARED;

@Component
@Log4j2
public class GeoserverServiceImpl implements GeoserverService {

    private static final String RASTER_STYLE = "raster";

    private final GeoServerRESTPublisher publisher;
    private final GeoServerRESTReader reader;

    @Value("${ftep.catalogue.geoserver.enabled:true}")
    private boolean geoserverEnabled;

    @Autowired
    public GeoserverServiceImpl(@Value("${ftep.catalogue.geoserver.url:http://ftep-geoserver:9080/geoserver/}") String url,
                                @Value("${ftep.catalogue.geoserver.username:ftepgeoserver}") String username,
                                @Value("${ftep.catalogue.geoserver.password:ftepgeoserver}") String password) throws MalformedURLException {
        GeoServerRESTManager geoserver = new GeoServerRESTManager(new URL(url), username, password);
        this.publisher = geoserver.getPublisher();
        this.reader = geoserver.getReader();
    }

    @Override
    public void ingest(String workspace, Path path, String crs) {
        Path fileName = path.getFileName();
        String datastoreName = MoreFiles.getNameWithoutExtension(fileName);
        String layerName = MoreFiles.getNameWithoutExtension(fileName);

        if (!geoserverEnabled) {
            LOG.warn("Geoserver is disabled; 'ingested' file: {}:{}", workspace, layerName);
            return;
        }

        ensureWorkspaceExists(workspace);

        if (!fileName.toString().toUpperCase().endsWith(".TIF")) {
            // TODO Ingest more filetypes
            throw new UnsupportedOperationException("Unable to ingest a non-GeoTiff product");
        }

        try {
            publisher.publishExternalGeoTIFF(workspace, datastoreName, path.toFile(), layerName, crs, REPROJECT_TO_DECLARED, RASTER_STYLE);
            LOG.info("Ingested GeoTIFF to geoserver with id: {}:{}", workspace, layerName);
        } catch (FileNotFoundException e) {
            LOG.error("Geoserver was unable to publish file: {}", path, e);
            throw new IllegalArgumentException(e);
        }
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

}
