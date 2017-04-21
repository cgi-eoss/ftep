package com.cgi.eoss.ftep.catalogue.files;

import com.cgi.eoss.ftep.catalogue.CatalogueUri;
import com.cgi.eoss.ftep.catalogue.geoserver.GeoserverService;
import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.catalogue.util.GeoUtil;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.MoreFiles;
import lombok.extern.log4j.Log4j2;
import org.geojson.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Component
@Log4j2
public class FilesystemOutputProductService implements OutputProductService {
    private final Path outputProductBasedir;
    private final RestoService resto;
    private final GeoserverService geoserver;

    @Autowired
    public FilesystemOutputProductService(@Qualifier("outputProductBasedir") Path outputProductBasedir, RestoService resto, GeoserverService geoserver) {
        this.outputProductBasedir = outputProductBasedir;
        this.resto = resto;
        this.geoserver = geoserver;
    }

    @Override
    public FtepFile ingest(User owner, String jobId, String crs, String geometry, Map<String, Object> properties, Path src) throws IOException {
        String filename = src.getFileName().toString();
        Path dest = outputProductBasedir.resolve(jobId).resolve(filename);

        if (!src.equals(dest)) {
            if (Files.exists(dest)) {
                LOG.warn("Found already-existing output product, overwriting: {}", dest);
            }

            Files.createDirectories(dest.getParent());
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        LOG.info("Ingesting output at {}", dest);

        geoserver.ingest(jobId, dest, crs);

        Feature feature = new Feature();
        feature.setId(jobId + "_" + filename);
        feature.setGeometry(GeoUtil.getGeoJsonGeometry(geometry));
        feature.setProperties(properties);

        UUID restoId = resto.ingestOutputProduct(feature);
        URI uri = CatalogueUri.OUTPUT_PRODUCT.build(
                ImmutableMap.of(
                        "jobId", jobId,
                        "filename", filename));
        LOG.info("Ingested product with Resto id {} and URI {}", restoId, uri);

        FtepFile ftepFile = new FtepFile(uri, restoId);
        ftepFile.setOwner(owner);
        ftepFile.setType(FtepFile.Type.OUTPUT_PRODUCT);
        ftepFile.setFilename(outputProductBasedir.relativize(dest).toString());
        return ftepFile;
    }

    @Override
    public Path provision(String jobId, String filename) throws IOException {
        Path outputPath = outputProductBasedir.resolve(jobId).resolve(filename);
        if (Files.exists(outputPath)) {
            LOG.warn("Found already-existing output product, may be overwritten: {}", outputPath);
        }
        Files.createDirectories(outputPath.getParent());
        return outputPath;
    }

    @Override
    public Resource resolve(FtepFile file) {
        Path path = outputProductBasedir.resolve(file.getFilename());
        return new PathResource(path);
    }

    @Override
    public void delete(FtepFile file) throws IOException {
        Path relativePath = Paths.get(file.getFilename());

        Files.deleteIfExists(outputProductBasedir.resolve(relativePath));

        resto.deleteReferenceData(file.getRestoId());

        // Workspace = jobId = first part of the relative filename
        String workspace = relativePath.getName(0).toString();
        // Layer name = filename without extension
        String layerName = MoreFiles.getNameWithoutExtension(relativePath.getFileName());
        geoserver.delete(workspace, layerName);
    }

}
