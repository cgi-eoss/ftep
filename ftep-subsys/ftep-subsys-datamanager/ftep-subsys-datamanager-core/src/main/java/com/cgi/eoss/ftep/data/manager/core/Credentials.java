package com.cgi.eoss.ftep.data.manager.core;

public class Credentials {
    private final String username;
    private final String password;
    private final String certificatePath;

    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
        this.certificatePath = null;
    }

    public Credentials(String certificatePath) {
        this.username = null;
        this.password = null;
        this.certificatePath = certificatePath;
    }

    public String toParameterString() {
        return username + ":" + password;
    }

}
