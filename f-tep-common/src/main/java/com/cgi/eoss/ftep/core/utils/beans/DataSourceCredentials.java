package com.cgi.eoss.ftep.core.utils.beans;

import lombok.Data;

@Data
public class DataSourceCredentials {

    private String sourceName;
    private String authenticationPolicy;
    private String certificatePath;
    private String userName;
    private String password;

}
