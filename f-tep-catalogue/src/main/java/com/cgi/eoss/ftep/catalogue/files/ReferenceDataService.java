package com.cgi.eoss.ftep.catalogue.files;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ReferenceDataService {
    FtepFile store(User user, String filename, String geometry, MultipartFile multipartFile) throws IOException;
}
