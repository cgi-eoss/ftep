package com.cgi.eoss.ftep.model;

import org.springframework.security.core.GrantedAuthority;

/**
 * <p>F-TEP application role for users, for highest-level access control.</p>
 */
public enum Role implements GrantedAuthority {

    GUEST,
    USER,
    EXPERT_USER,
    CONTENT_AUTHORITY,
    ADMIN;

    private static final String ROLE_AUTHORITY_PREFIX = "ROLE_";

    @Override
    public String getAuthority() {
        return ROLE_AUTHORITY_PREFIX + toString();
    }

    public static String getRoleAuthorityPrefix() {
        return ROLE_AUTHORITY_PREFIX;
    }

}
