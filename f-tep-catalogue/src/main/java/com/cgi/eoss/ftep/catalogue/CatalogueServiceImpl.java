package com.cgi.eoss.ftep.catalogue;

import com.cgi.eoss.ftep.catalogue.files.ReferenceDataService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class CatalogueServiceImpl implements CatalogueService {

    private final ReferenceDataService referenceDataService;

    @Autowired
    public CatalogueServiceImpl(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    @Override
    public FtepFile createReferenceData(User user, String filename, String geometry, MultipartFile file) throws IOException {
        return referenceDataService.store(user, filename, geometry, file);
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

}
