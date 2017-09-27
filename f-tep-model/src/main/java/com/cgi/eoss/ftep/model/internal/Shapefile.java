package com.cgi.eoss.ftep.model.internal;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.Set;

@Data
@Builder
public class Shapefile {

    public Path zip;
    public Set<Path> contents;

}
