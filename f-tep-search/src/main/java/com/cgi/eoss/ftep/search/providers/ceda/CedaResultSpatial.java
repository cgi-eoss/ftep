package com.cgi.eoss.ftep.search.providers.ceda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.geojson.LngLatAlt;

import java.util.List;

/**
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class CedaResultSpatial {
    @JsonProperty("geometries")
    private Geometries geometries;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Geometries {
        @JsonProperty("display")
        private CedaResultSpatial.Geometry display;
        @JsonProperty("search")
        private CedaResultSpatial.Geometry search;
        @JsonProperty("full_search")
        private CedaResultSpatial.Geometry fullSearch;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Geometry {
        @JsonProperty("orientation")
        private String orientation;
        @JsonProperty("type")
        private String type;
        @JsonProperty("coordinates")
        private List<List<LngLatAlt>> coordinates;
    }
}
