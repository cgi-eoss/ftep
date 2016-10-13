package com.cgi.eoss.ftep.core.utils.beans;

/**
 * Class wrapping the string representation of a ZOO-Project job parameter ID, for type-safe ID handling.
 */
public final class ParameterId {

    private final String id;

    private ParameterId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static ParameterId of(String id) {
        return new ParameterId(id);
    }
}
