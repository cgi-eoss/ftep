package com.cgi.eoss.ftep.catalogue.files;

import com.cgi.eoss.ftep.catalogue.CatalogueUri;
import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.catalogue.util.GeoUtil;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import com.google.common.collect.ImmutableMap;
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
import java.util.Map;
import java.util.UUID;

@Log4j2
@Component
public class FilesystemReferenceDataService implements ReferenceDataService {

    private final Path referenceDataBasedir;
    private final RestoService resto;

    @Autowired
    public FilesystemReferenceDataService(@Qualifier("referenceDataBasedir") Path referenceDataBasedir, RestoService resto) {
        this.referenceDataBasedir = referenceDataBasedir;
        this.resto = resto;
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

        Feature feature = new Feature();
        feature.setId(owner.getName() + "_" + filename);
        feature.setGeometry(GeoUtil.getGeoJsonGeometry(geometry));
        // TODO Add internally-generated properties?
        feature.setProperties(userProperties);

        UUID restoId = resto.ingestReferenceData(feature);
        URI uri = CatalogueUri.REFERENCE_DATA.build(
                ImmutableMap.of(
                        "ownerId", owner.getId().toString(),
                        "filename", filename));
        LOG.info("Ingested product with Resto id {} and URI {}", restoId, uri);

        FtepFile ftepFile = new FtepFile(uri, restoId);
        ftepFile.setOwner(owner);
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
