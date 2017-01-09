package com.cgi.eoss.ftep.model.internal;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Credentials {
    private String certificate;
    private String username;
    private String password;

    public boolean isBasicAuth() {
        return !Strings.isNullOrEmpty(username);
    }
}
