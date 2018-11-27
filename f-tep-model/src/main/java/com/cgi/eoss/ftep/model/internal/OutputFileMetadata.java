package com.cgi.eoss.ftep.model.internal;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class OutputFileMetadata {
    private OutputProductMetadata outputProductMetadata;
    private String crs;
    private String geometry;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
}
