package com.cgi.eoss.ftep.model.internal;

import lombok.Data;

import java.nio.file.Path;

@Data
public class RetrievedOutputFile {
    private final OutputFileMetadata outputFileMetadata;
    private final Path path;
}
