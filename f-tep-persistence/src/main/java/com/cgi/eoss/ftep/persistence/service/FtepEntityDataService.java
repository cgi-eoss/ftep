package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepEntity;

/**
 * A directory to store, load and delete FtepEntity objects. Provides data integrity and constraint checks
 * before passing to the DAO.
 *
 * @param <T> The data type to be provided.
 */
public interface FtepEntityDataService<T extends FtepEntity<T>> extends DataService<T, Long> {

}
