package com.cgi.eoss.ftep.search.providers.resto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.geojson.Feature;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestoResult {
    @JsonProperty("type")
    private String type;
    @JsonProperty("properties")
    private RestoResultProperties properties;
    @JsonProperty("features")
    private List<Feature> features;
}
