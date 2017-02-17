package com.cgi.eoss.ftep.api.security;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

import java.util.Set;

public enum FtepPermission {

    READ,
    WRITE,
    ADMIN;

    private static final BiMap<FtepPermission, Set<Permission>> SPRING_FTEP_PERMISSION_MAP = ImmutableBiMap.<FtepPermission, Set<Permission>>builder()
            .put(FtepPermission.READ, ImmutableSet.of(BasePermission.READ))
            .put(FtepPermission.WRITE, ImmutableSet.of(BasePermission.WRITE, BasePermission.READ))
            .put(FtepPermission.ADMIN, ImmutableSet.of(BasePermission.ADMINISTRATION, BasePermission.WRITE, BasePermission.READ))
            .build();

    public Set<Permission> getAclPermissions() {
        return SPRING_FTEP_PERMISSION_MAP.get(this);
    }

    public static FtepPermission getFtepPermission(Set<Permission> aclPermissions) {
        return SPRING_FTEP_PERMISSION_MAP.inverse().get(aclPermissions);
    }

}
