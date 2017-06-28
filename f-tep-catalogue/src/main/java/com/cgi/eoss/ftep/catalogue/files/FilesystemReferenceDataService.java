package com.cgi.eoss.ftep.catalogue.files;

import com.cgi.eoss.ftep.catalogue.CatalogueUri;
import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.catalogue.util.GeoUtil;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;
import lombok.extern.log4j.Log4j2;
import org.geojson.Feature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Component
public class FilesystemReferenceDataService implements ReferenceDataService {

    private final Path referenceDataBasedir;
    private final RestoService resto;
    private final ObjectMapper jsonMapper;

    @Autowired
    public FilesystemReferenceDataService(@Qualifier("referenceDataBasedir") Path referenceDataBasedir, RestoService resto, ObjectMapper jsonMapper) {
        this.referenceDataBasedir = referenceDataBasedir;
        this.resto = resto;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public FtepFile ingest(User owner, String filename, String geometry, Map<String, Object> userProperties, MultipartFile multipartFile) throws IOException {
        Path dest = referenceDataBasedir.resolve(String.valueOf(owner.getId())).resolve(filename);
        LOG.info("Saving new reference data to: {}", dest);

        if (Files.exists(dest)) {
            LOG.warn("Found already-existing reference data, overwriting: {}", dest);
        }

        Files.createDirectories(dest.getParent());
        Files.copy(multipartFile.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        URI uri = CatalogueUri.REFERENCE_DATA.build(
                ImmutableMap.of(
                        "ownerId", owner.getId().toString(),
                        "filename", filename));

        Map<String, Object> properties = new HashMap<>();

        // Add automatically-determined properties
        properties.put("productIdentifier", owner.getName() + "_" + filename);
        properties.put("owner", owner.getName());
        properties.put("filename", filename);
        properties.put("ftepUrl", uri);
        // TODO Get the proper MIME type
        properties.put("resourceMimeType", "application/unknown");
        properties.put("resourceSize", Files.size(dest));
        properties.put("resourceChecksum", "sha256=" + MoreFiles.asByteSource(dest).hash(Hashing.sha256()));
        // TODO Validate extra properties?
        properties.put("extraParams", jsonMapper.writeValueAsString(userProperties));

        Feature feature = new Feature();
        feature.setId(owner.getName() + "_" + filename);
        feature.setGeometry(GeoUtil.getGeoJsonGeometry(geometry));
        feature.setProperties(properties);

        UUID restoId;
        try {
            restoId = resto.ingestReferenceData(feature);
            LOG.info("Ingested reference data with Resto id {} and URI {}", restoId, uri);
        } catch (Exception e) {
            LOG.error("Failed to ingest reference data to Resto, continuing...", e);
            // TODO Add GeoJSON to FtepFile model
            restoId = UUID.randomUUID();
        }

        FtepFile ftepFile = new FtepFile(uri, restoId);
        ftepFile.setOwner(owner);
        ftepFile.setType(FtepFile.Type.REFERENCE_DATA);
        ftepFile.setFilename(referenceDataBasedir.relativize(dest).toString());
        return ftepFile;
    }

    @Override
    public Resource resolve(FtepFile file) {
        Path path = referenceDataBasedir.resolve(file.getFilename());
        return new PathResource(path);
    }

    @Override
    public void delete(FtepFile file) throws IOException {
        Files.deleteIfExists(referenceDataBasedir.resolve(file.getFilename()));
        resto.deleteReferenceData(file.getRestoId());
    }

}
