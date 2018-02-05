package com.cgi.eoss.ftep.search.providers.ceda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class CedaResultDataFormat {
    @JsonProperty("format")
    private String format;
}
