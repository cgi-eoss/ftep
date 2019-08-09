package com.cgi.eoss.ftep.catalogue.external;

import com.cgi.eoss.ftep.catalogue.CatalogueUri;
import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Striped;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

/**
 * <p>Handler for external product (e.g. S-1, S-2, Landsat) metadata and files.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class ExternalProductDataServiceImpl implements ExternalProductDataService {

    private final FtepFileDataService ftepFileDataService;
    private final RestoService resto;
    private final Striped<Lock> externalProductLock = Striped.lock(1);

    @Override
    public FtepFile ingest(GeoJsonObject geoJson) {
        // TODO Handle non-feature objects?
        Feature feature = (Feature) geoJson;

        String productSource = feature.getProperty("productSource").toString().toLowerCase().replaceAll("[^a-z0-9]", "");
        String productId = feature.getProperty("productIdentifier");

        Long filesize = Optional.ofNullable((Long) feature.getProperties().get("filesize")).orElse(getFilesize(feature));
        feature.getProperties().put("filesize", filesize);

        URI uri = Optional.ofNullable(feature.getProperties().get("ftepUrl")).map(Object::toString).map(URI::create).orElse(getUri(productSource, productId));
        feature.getProperties().put("ftepUrl", uri);

        Lock lock = externalProductLock.get(uri);
        lock.lock();
        try {
            Optional<FtepFile> existingFile = Optional.ofNullable(ftepFileDataService.getByUri(uri));

            // Update the existing file with new attributes, or save the new file
            return ftepFileDataService.save(existingFile.map(ftepFile -> {
                ftepFile.setFilename(productId);
                ftepFile.setFilesize(filesize);
                return ftepFile;
            }).orElseGet(() -> {
                UUID restoId;
                try {
                    restoId = resto.ingestExternalProduct(productSource, feature);
                    LOG.info("Ingested external product with Resto id {} and URI {}", restoId, uri);
                } catch (Exception e) {
                    LOG.error("Failed to ingest external product to Resto, continuing...", e);
                    // TODO Add GeoJSON to FtepFile model
                    restoId = UUID.randomUUID();
                }
                FtepFile ftepFile = new FtepFile(uri, restoId);
                ftepFile.setType(FtepFile.Type.EXTERNAL_PRODUCT);
                ftepFile.setFilename(productId);
                ftepFile.setFilesize(filesize);
                return ftepFile;
            }));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public URI getUri(String productSource, String productId) {
        URI uri;
        try {
            CatalogueUri productSourceUrl = CatalogueUri.valueOf(productSource);
            uri = productSourceUrl.build(ImmutableMap.of("productId", productId));
        } catch (IllegalArgumentException e) {
            uri = URI.create(productSource.replaceAll("[^a-z0-9+.-]", "-") + ":///" + productId);
            LOG.debug("Could not build a well-designed F-TEP URI handler, returning automatic: {}", uri, e);
        }
        return uri;
    }

    @SuppressWarnings("unchecked")
    private Long getFilesize(Feature feature) {
        return Optional.ofNullable((Map<String, Object>) feature.getProperties().get("extraParams"))
                .map(ep -> (Map<String, Object>) ep.get("file"))
                .map(file -> file.get("data_file_size"))
                .map(Object::toString)
                .map(Long::parseLong)
                .orElse(null);
    }

    @Override
    public Resource resolve(FtepFile file) {
        // TODO Allow proxied access (with TEP coin cost) to some external data
        throw new UnsupportedOperationException("Direct download of external products via F-TEP is not permitted");
    }

    @Override
    public void delete(FtepFile file) throws IOException {
        resto.deleteExternalData(file.getRestoId());
    }

}
