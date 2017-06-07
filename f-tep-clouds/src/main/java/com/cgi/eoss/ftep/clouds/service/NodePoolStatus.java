package com.cgi.eoss.ftep.clouds.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodePoolStatus {
    private final int maxPoolSize;
    private final int used;
}
