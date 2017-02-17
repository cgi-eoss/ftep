package com.cgi.eoss.ftep.persistence.service;

import org.springframework.core.convert.converter.Converter;

import java.util.Collection;
import java.util.List;

/**
 */
public interface DataService<T, I> extends Converter<I, T> {

    /**
     * Delete the specified object from the data store.
     */
    void delete(T obj);

    /**
     * Remove all persistent instances of this type.
     */
    void deleteAll();

    /**
     * @return All persistent instances of this type.
     */
    List<T> getAll();

    /**
     * @return All persistent instances of this type, with lazy-loaded properties initialised.
     */
    List<T> getAllFull();

    /**
     * Returns a persistent instance of the supplied object; either saving it in the database or retrieving the
     * already-stored instance.
     *
     * @param obj The object to be persisted (if necessary) and returned.
     * @return A persistent instance of the requested object.
     */
    T save(T obj);

    /**
     * @return The persistent instance matching the given unique identifier.
     */
    T getById(I id);

    /**
     * @return All persistent instances matching the given unique identifiers.
     */
    List<T> getByIds(Collection<I> ids);

    /**
     * @return True if the given object would be unique in the database.
     */
    boolean isUnique(T obj);

    /**
     * @return True if the given object is unique and valid for the purposes of updating.
     */
    boolean isUniqueAndValid(T obj);

    /**
     * Update all persistent objects in the given collection, returning the new objects.
     */
    Collection<T> save(Collection<T> objs);

    /**
     * Resync the given object with its persistent representation. This may be useful when certain properties are set or
     * populated by database triggers.
     */
    T refresh(T obj);

    /**
     * Resync the given object with its database representation, performing a full eviction and reload, including all
     * lazy-loaded properties.
     */
    T refreshFull(T obj);
}
