package com.cgi.eoss.ftep.search.ftep.opensearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.geojson.Feature;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestoSearchResult {
    @JsonProperty("type")
    private String type;
    @JsonProperty("properties")
    private RestoSearchResultProperties properties;
    @JsonProperty("features")
    private List<Feature> features;
}
