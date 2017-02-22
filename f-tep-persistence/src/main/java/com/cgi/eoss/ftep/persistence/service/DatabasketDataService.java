package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;

import java.util.List;

public interface DatabasketDataService extends
        FtepEntityDataService<Databasket>,
        SearchableDataService<Databasket> {
    Databasket getByNameAndOwner(String name, User user);

    List<Databasket> findByFile(FtepFile file);

    List<Databasket> findByOwner(User user);
}
