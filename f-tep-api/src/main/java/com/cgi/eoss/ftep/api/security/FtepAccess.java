package com.cgi.eoss.ftep.api.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FtepAccess {
    private final boolean published;
    private final FtepPermission currentLevel;
}
