package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Searchable;

import java.util.List;

/**
 * <p>A data directory allowing simple string-based searching for entities.</p>
 */
@FunctionalInterface
interface SearchableDataService<T extends Searchable> {

    /**
     * @return All entities matching the given search term across one or more fields. The set of searched fields depends
     * on the entity.
     */
    List<T> search(String term);

}
