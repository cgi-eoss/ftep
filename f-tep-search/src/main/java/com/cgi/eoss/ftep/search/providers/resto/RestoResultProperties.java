package com.cgi.eoss.ftep.search.providers.resto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestoResultProperties {
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
    private RestoResultQuery query;
    @JsonProperty("links")
    private RestoResultPropertiesLink[] links;
}
