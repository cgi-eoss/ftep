package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepFileType;
import com.cgi.eoss.ftep.model.User;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public interface FtepFileDataService extends
        FtepEntityDataService<FtepFile> {
    FtepFile getByUri(URI uri);

    FtepFile getByRestoId(UUID uuid);

    List<FtepFile> findByOwner(User user);

    List<FtepFile> getByType(FtepFileType type);
}
