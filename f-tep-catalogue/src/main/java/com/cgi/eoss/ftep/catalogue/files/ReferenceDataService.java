package com.cgi.eoss.ftep.catalogue.files;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface ReferenceDataService extends FtepFileService {
    FtepFile ingest(User user, String filename, String geometry, Map<String, Object> properties, MultipartFile multipartFile) throws IOException;
}
