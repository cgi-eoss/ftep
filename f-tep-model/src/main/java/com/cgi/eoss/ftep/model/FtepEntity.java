package com.cgi.eoss.ftep.model;

import org.springframework.hateoas.Identifiable;

public interface FtepEntity<T> extends Comparable<T>, Identifiable<Long> {

    /**
     * @return The unique identifier of the entity.
     */
    Long getId();

    /**
     * @param id The unique identifier of the entity.
     */
    void setId(Long id);

}
