package com.cgi.eoss.ftep.search.ftep.opensearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestoSearchResultProperties {
    @JsonProperty("id")
    private String id;
    @JsonProperty("totalResults")
    private long totalResultsCount;
    @JsonProperty("exactCount")
    private boolean exactCount;
    @JsonProperty("startIndex")
    private long startIndex;
    @JsonProperty("itemsPerPage")
    private long itemsPerPage;
    @JsonProperty("query")
    private RestoSearchQuery query;
    @JsonProperty("links")
    private RestoSearchPropertiesLink[] links;
}
