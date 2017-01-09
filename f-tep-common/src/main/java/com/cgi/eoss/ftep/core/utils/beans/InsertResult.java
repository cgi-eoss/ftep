package com.cgi.eoss.ftep.core.utils.beans;

import lombok.Data;

@Data
public class InsertResult {

    private boolean success = false;
    private String resourceRestEndpoint;
    private String resourceId;

    @Override
    public String toString() {
        return "InsertResult{" + "status='" + success + '\'' + ", resourceRestEndpoint='"
                + resourceRestEndpoint + '\'' + ", resourceId=" + resourceId + '}';
    }

}
