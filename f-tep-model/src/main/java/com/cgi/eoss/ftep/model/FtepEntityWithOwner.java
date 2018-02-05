package com.cgi.eoss.ftep.model;

public interface FtepEntityWithOwner<T> extends FtepEntity<T> {

    /**
     * @return The user who owns the entity.
     */
    User getOwner();

    /**
     * @param owner The new owner of the entity.
     */
    void setOwner(User owner);

}
