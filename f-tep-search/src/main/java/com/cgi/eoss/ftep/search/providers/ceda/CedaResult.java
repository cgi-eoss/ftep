package com.cgi.eoss.ftep.search.providers.ceda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CedaResult {
    @JsonProperty("totalResults")
    private long totalResults;
    @JsonProperty("startIndex")
    private long startIndex;
    @JsonProperty("rows")
    private List<CedaResultRow> rows;
}
