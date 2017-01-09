package com.cgi.eoss.ftep.orchestrator;

public class ServiceExecutionException extends RuntimeException {
    public ServiceExecutionException(Throwable cause) {
        super(cause);
    }

    public ServiceExecutionException(String message) {
        super(message);
    }
}
