package com.cgi.eoss.ftep.search.scihub.opensearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenSearchEntryLink {
    @JsonProperty("rel")
    private String relation;
    @JsonProperty("href")
    private String href;
    @JsonProperty("type")
    private String type;
}