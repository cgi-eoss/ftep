package com.cgi.eoss.ftep.catalogue.files;

import com.cgi.eoss.ftep.catalogue.FtepFileService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import okhttp3.HttpUrl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface OutputProductService extends FtepFileService {
    FtepFile ingest(User owner, String jobId, String crs, String geometry, Map<String, Object> properties, Path path) throws IOException;

    Path provision(String jobId, String filename) throws IOException;

    HttpUrl getWmsUrl(String jobId, String filename);
}
