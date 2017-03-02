package com.cgi.eoss.ftep.catalogue.geoserver;

import com.google.common.io.Files;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import static it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy.REPROJECT_TO_DECLARED;

@Component
@Slf4j
public class GeoserverServiceImpl implements GeoserverService {

    private static final String RASTER_STYLE = "raster";

    private final GeoServerRESTPublisher publisher;
    private final GeoServerRESTReader reader;

    @Autowired
    public GeoserverServiceImpl(@Value("${ftep.catalogue.geoserver.url:http://ftep-geoserver:9080/}") String url,
                                @Value("${ftep.catalogue.geoserver.username:ftepgeoserver}") String username,
                                @Value("${ftep.catalogue.geoserver.password:ftepgeoserver}") String password) throws MalformedURLException {
        GeoServerRESTManager geoserver = new GeoServerRESTManager(new URL(url), username, password);
        this.publisher = geoserver.getPublisher();
        this.reader = geoserver.getReader();
    }

    @Override
    public void ingest(String workspace, Path path, String crs) {
        ensureWorkspaceExists(workspace);
        String filename = path.getFileName().toString();

        if (!filename.toUpperCase().endsWith(".TIF")) {
            // TODO Ingest more filetypes
            throw new UnsupportedOperationException("Unable to ingest a non-GeoTiff product");
        }

        String datastoreName = Files.getNameWithoutExtension(filename);
        String layerName = Files.getNameWithoutExtension(filename);

        try {
            publisher.publishExternalGeoTIFF(workspace, datastoreName, path.toFile(), layerName, crs, REPROJECT_TO_DECLARED, RASTER_STYLE);
            LOG.info("Ingested GeoTIFF to geoserver with id: {}:{}", workspace, layerName);
        } catch (FileNotFoundException e) {
            LOG.error("Geoserver was unable to publish file: {}", path, e);
            throw new IllegalArgumentException(e);
        }
    }

    private void ensureWorkspaceExists(String workspace) {
        if (!reader.existsWorkspace(workspace)) {
            LOG.info("Creating new workspace {}", workspace);
            publisher.createWorkspace(workspace);
        }
    }

}
