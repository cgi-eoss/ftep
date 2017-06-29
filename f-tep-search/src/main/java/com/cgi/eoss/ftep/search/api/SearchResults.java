package com.cgi.eoss.ftep.search.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.geojson.Feature;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class SearchResults {
    @JsonProperty
    private SearchParameters parameters;
    @JsonProperty
    private Page page;
    @JsonProperty("_links")
    private Map<String, Link> links;
    @JsonProperty
    private List<Feature> features;

    @Data
    @Builder
    public static final class Page {
        @JsonProperty
        private long size;
        @JsonProperty
        private long totalElements;
        @JsonProperty
        private long totalPages;
        @JsonProperty
        private long number;
    }

    @Data
    @Builder
    public static final class Link {
        @JsonProperty
        private String rel;
        @JsonProperty
        private String href;
        @JsonProperty
        private String type;
    }
}