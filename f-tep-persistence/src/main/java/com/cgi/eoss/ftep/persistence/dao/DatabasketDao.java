package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface DatabasketDao extends FtepEntityDao<Databasket> {
    Databasket findOneByNameAndOwner(String name, User user);

    List<Databasket> findByNameContainingIgnoreCase(String term);

    List<Databasket> findByFilesContaining(FtepFile file);

    List<Databasket> findByOwner(User user);

    List<Databasket> findByFiles(FtepFile file);
}
