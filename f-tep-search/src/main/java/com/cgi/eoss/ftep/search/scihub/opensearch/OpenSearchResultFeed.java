package com.cgi.eoss.ftep.search.scihub.opensearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenSearchResultFeed {
    @JsonProperty("opensearch:startIndex")
    private long startIndex;
    @JsonProperty("opensearch:totalResults")
    private long totalResultsCount;
    @JsonProperty("id")
    private String id;
    @JsonProperty("link")
    private List<OpenSearchEntryLink> links;
    @JsonProperty("entry")
    private List<OpenSearchEntry> entries;
}
