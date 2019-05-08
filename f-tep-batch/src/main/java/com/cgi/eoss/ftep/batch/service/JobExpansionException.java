package com.cgi.eoss.ftep.batch.service;

/**
 * An exception throw during the execution of expanding JobParams
 */
public class JobExpansionException extends RuntimeException {

    /**
     * <p>Constructs a new job expansion exception with the given detail message.</p>
     *
     * @param message
     */
    public JobExpansionException(String message) {
        super(message);
    }

    /**
     * <p>Constructs a new job expansion exception with the given detail message and cause.</p>
     *
     * @param message
     * @param cause
     */
    public JobExpansionException(String message, Throwable cause) {
        super(message, cause);
    }


}
