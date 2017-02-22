package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public interface FtepFileDao extends FtepEntityDao<FtepFile> {
    FtepFile findOneByUri(URI uri);

    FtepFile findOneByRestoId(UUID uuid);

    List<FtepFile> findByOwner(User user);
}
