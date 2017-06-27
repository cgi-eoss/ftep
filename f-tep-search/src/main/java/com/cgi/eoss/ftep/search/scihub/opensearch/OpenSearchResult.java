package com.cgi.eoss.ftep.search.scihub.opensearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OpenSearchResult {
    @JsonProperty("feed")
    private OpenSearchResultFeed feed;
}
