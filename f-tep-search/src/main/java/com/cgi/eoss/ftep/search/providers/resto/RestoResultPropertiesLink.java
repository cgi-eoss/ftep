package com.cgi.eoss.ftep.search.providers.resto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestoResultPropertiesLink {
    @JsonProperty("rel")
    private String relation;
    @JsonProperty("href")
    private String href;
    @JsonProperty("type")
    private String type;
}