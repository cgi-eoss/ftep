package com.cgi.eoss.ftep.search.providers.scihub;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OpenSearchResult {
    @JsonProperty("feed")
    private OpenSearchResultFeed feed;
}
