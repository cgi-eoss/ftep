package com.cgi.eoss.ftep.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FtepAccess {
    private final boolean published;
    private final boolean publishRequested;
    private final FtepPermission currentLevel;
}
