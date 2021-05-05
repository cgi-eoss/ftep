package com.cgi.eoss.ftep.security;

import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

public class FtepCustomPermission extends BasePermission {

    public static final Permission READ_SERVICE_FILES = new FtepCustomPermission(1<<5,'F'); // 32
    public static final Permission LAUNCH = new FtepCustomPermission(1<<6,'L'); // 64

    protected FtepCustomPermission(int mask) {
        super(mask);
    }

    protected FtepCustomPermission(int mask, char code) {
        super(mask, code);
    }
}
