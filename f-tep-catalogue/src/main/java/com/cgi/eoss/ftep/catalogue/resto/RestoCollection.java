package com.cgi.eoss.ftep.catalogue.resto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * <p>JSON-Object mapping for a Resto Collection.</p>
 */
@Data
@Builder
public class RestoCollection {
    private String name;
    private String model;
    private String status;
    private String licenseId;
    private Map<String, Integer> rights;
    private Map<String, OpensearchDescription> osDescription;
    private Map<String, String> propertiesMapping;

    @Data
    @Builder
    public static final class OpensearchDescription {
        @JsonProperty("ShortName")
        private String shortName;
        @JsonProperty("LongName")
        private String longName;
        @JsonProperty("Description")
        private String description;
        @JsonProperty("Tags")
        private String tags;
        @JsonProperty("Developer")
        private String developer;
        @JsonProperty("Contact")
        private String contact;
        @JsonProperty("Query")
        private String query;
        @JsonProperty("Attribution")
        private String attribution;
    }

}
