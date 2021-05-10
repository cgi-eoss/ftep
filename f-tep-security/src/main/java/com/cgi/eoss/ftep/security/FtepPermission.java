package com.cgi.eoss.ftep.security;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

public enum FtepPermission {

    READ,
    WRITE,
    ADMIN,
    SERVICE_READONLY_DEVELOPER,             // view service files
    SERVICE_DEVELOPER,                      // view and update service files
    SERVICE_OPERATOR,                       // view and update service files and launch the service
    SERVICE_USER,                           // launch the service
    /**
     * <p>A marker permission for API access information; not used to map Spring ACLs.</p>
     */
    SUPERUSER;

    private static final BiMap<FtepPermission, Set<Permission>> SPRING_FTEP_PERMISSION_MAP = ImmutableBiMap.<FtepPermission, Set<Permission>>builder()
            .put(FtepPermission.READ, ImmutableSet.of(BasePermission.READ))
            .put(FtepPermission.WRITE, ImmutableSet.of(BasePermission.WRITE, BasePermission.READ))
            .put(FtepPermission.ADMIN, ImmutableSet.of(BasePermission.ADMINISTRATION, BasePermission.WRITE, BasePermission.READ))
            .put(FtepPermission.SERVICE_READONLY_DEVELOPER, ImmutableSet.of(FtepCustomPermission.READ_SERVICE_FILES, BasePermission.READ))
            .put(FtepPermission.SERVICE_DEVELOPER, ImmutableSet.of(FtepCustomPermission.READ_SERVICE_FILES, BasePermission.WRITE, BasePermission.READ))
            .put(FtepPermission.SERVICE_OPERATOR, ImmutableSet.of(FtepCustomPermission.LAUNCH, FtepCustomPermission.READ_SERVICE_FILES, BasePermission.ADMINISTRATION, BasePermission.WRITE, BasePermission.READ))
            .put(FtepPermission.SERVICE_USER, ImmutableSet.of(FtepCustomPermission.LAUNCH, BasePermission.READ))
            .build();

    /**
     * <p>A Spring Security GrantedAuthority for PUBLIC visibility. Not technically an FtepPermission enum value, but
     * may be treated similarly.</p>
     */
    public static final GrantedAuthority PUBLIC = new SimpleGrantedAuthority("PUBLIC");

    public Set<Permission> getAclPermissions() {
        return SPRING_FTEP_PERMISSION_MAP.get(this);
    }

    public static FtepPermission getFtepPermission(Set<Permission> aclPermissions) {
        return SPRING_FTEP_PERMISSION_MAP.inverse().get(aclPermissions);
    }

}
