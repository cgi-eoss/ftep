package com.cgi.eoss.ftep.search.providers.ceda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class CedaResultRow {
    @JsonProperty("data_format")
    private CedaResultDataFormat dataFormat;
    @JsonProperty("misc")
    private Map<String, Object> misc;
    @JsonProperty("temporal")
    private CedaResultTemporal temporal;
    @JsonProperty("spatial")
    private CedaResultSpatial spatial;
    @JsonProperty("file")
    private CedaResultFile file;
}
