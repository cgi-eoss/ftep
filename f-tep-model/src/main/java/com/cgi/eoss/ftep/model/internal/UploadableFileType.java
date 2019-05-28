package com.cgi.eoss.ftep.model.internal;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * <p>Well-known file types which may be handled by f-tep-catalogue, e.g. Reference Data, Input or Output Products.</p>
 */
public enum UploadableFileType {
    GEOJSON(false, ImmutableSet.of("json", "geojson")),
    GEOTIFF(true, ImmutableSet.of("tif", "tiff")),
    KML(false, ImmutableSet.of("kml", "kmz")),
    SHAPEFILE(true, ImmutableSet.of("zip")),
    OTHER(false, Collections.emptySet());

    @Getter
    private final boolean autoDetectFlag;
    @Getter
    private final Set<String> fileExtensions;

    UploadableFileType(boolean autoDetectFlag, Set<String> fileExtensions) {
        this.autoDetectFlag = autoDetectFlag;
        this.fileExtensions = fileExtensions;
    }

    public static UploadableFileType getFileTypeByExtension(String extension) {
        return Arrays.stream(UploadableFileType.values())
                .filter(filetype -> filetype.fileExtensions.contains(extension.toLowerCase()))
                .sorted()
                .findFirst()
                .orElse(OTHER);
    }

}
