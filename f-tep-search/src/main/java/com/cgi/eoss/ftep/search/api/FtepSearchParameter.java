package com.cgi.eoss.ftep.search.api;

/**
 * <p>A general-purpose search parameter for use by F-TEP clients.</p>
 *
 * @param <T> The parameter value type.
 */
public interface FtepSearchParameter<T> {

    String getKey();

    T getValue();

}
