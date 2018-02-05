package com.cgi.eoss.ftep.search.providers.ceda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class CedaResultFile {
    @JsonProperty("size")
    private long size;
    @JsonProperty("directory")
    private String directory;
    @JsonProperty("quicklook_file")
    private String quicklookFile;
    @JsonProperty("location")
    private String location;
    @JsonProperty("path")
    private String path;
    @JsonProperty("data_file_size")
    private long dataFileSize;
    @JsonProperty("data_file")
    private String dataFile;
    @JsonProperty("data_file_sizes")
    private String dataFileSizes;
    @JsonProperty("data_files")
    private String dataFiles;
    @JsonProperty("metadata_file")
    private String metadataFile;
    @JsonProperty("filename")
    private String filename;
}
