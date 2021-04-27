package com.cgi.eoss.ftep.security;

import org.springframework.security.acls.domain.DefaultPermissionFactory;

public class FtepCustomPermissionFactory extends DefaultPermissionFactory {

    public FtepCustomPermissionFactory() {
        super();
        registerPublicPermissions(FtepCustomPermission.class);
    }
}
