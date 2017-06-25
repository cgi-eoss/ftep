package com.cgi.eoss.ftep.search.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.geojson.Feature;

import java.util.List;

@Data
@Builder
public class SearchResults {
    @JsonProperty
    private SearchParameters parameters;
    @JsonProperty
    private Paging paging;
    @JsonProperty
    private List<Link> links;
    @JsonProperty
    private List<Feature> features;

    @Data
    @Builder
    public static final class Paging {
        @JsonProperty
        private long totalResults;
        @JsonProperty
        private boolean exactCount;
        @JsonProperty
        private long startIndex;
        @JsonProperty
        private long itemsPerPage;
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