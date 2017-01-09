package com.cgi.eoss.ftep.model;

public interface FtepEntity<T> extends Comparable<T> {

    /**
     * @return The unique identifier of the entity.
     */
    Long getId();

    /**
     * @param id The unique identifier of the entity.
     */
    void setId(Long id);

}
