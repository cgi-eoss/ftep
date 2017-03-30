package com.cgi.eoss.ftep.catalogue;

public class IngestionException extends RuntimeException {
    public IngestionException(String message) {
        super(message);
    }

    public IngestionException(Throwable cause) {
        super(cause);
    }

    public IngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
