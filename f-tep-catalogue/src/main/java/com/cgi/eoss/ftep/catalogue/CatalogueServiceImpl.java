package com.cgi.eoss.ftep.catalogue;

import com.cgi.eoss.ftep.catalogue.files.OutputProductService;
import com.cgi.eoss.ftep.catalogue.files.ReferenceDataService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.internal.OutputProductMetadata;
import com.cgi.eoss.ftep.model.internal.ReferenceDataMetadata;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class CatalogueServiceImpl implements CatalogueService {

    private final FtepFileDataService ftepFileDataService;
    private final OutputProductService outputProductService;
    private final ReferenceDataService referenceDataService;

    @Autowired
    public CatalogueServiceImpl(FtepFileDataService ftepFileDataService, OutputProductService outputProductService, ReferenceDataService referenceDataService) {
        this.ftepFileDataService = ftepFileDataService;
        this.outputProductService = outputProductService;
        this.referenceDataService = referenceDataService;
    }

    @Override
    public FtepFile ingestReferenceData(ReferenceDataMetadata referenceData, MultipartFile file) throws IOException {
        FtepFile ftepFile = referenceDataService.ingest(referenceData.getOwner(), referenceData.getFilename(), referenceData.getGeometry(), referenceData.getProperties(), file);
        return ftepFileDataService.save(ftepFile);
    }

    @Override
    public FtepFile ingestOutputProduct(OutputProductMetadata outputProduct, Path path) throws IOException {
        FtepFile ftepFile = outputProductService.ingest(
                outputProduct.getOwner(),
                outputProduct.getJobId(),
                outputProduct.getCrs(),
                outputProduct.getGeometry(),
                outputProduct.getProperties(),
                path);
        return ftepFileDataService.save(ftepFile);
    }

    @Override
    public Resource getAsResource(FtepFile file) {
        switch (file.getType()) {
            case REFERENCE_DATA:
                return referenceDataService.resolve(file);
            default:
                throw new UnsupportedOperationException("Unable to materialise FtepFile: " + file);
        }
    }

    @Override
    public void delete(FtepFile file) {
        // TODO Implement deletion from external catalogues
        ftepFileDataService.delete(file);
    }

}
