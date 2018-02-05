package com.cgi.eoss.ftep.clouds.service;

public class NodeProvisioningException extends RuntimeException {
    public NodeProvisioningException(Throwable cause) {
        super(cause);
    }

    public NodeProvisioningException(String message) {
        super(message);
    }
}
