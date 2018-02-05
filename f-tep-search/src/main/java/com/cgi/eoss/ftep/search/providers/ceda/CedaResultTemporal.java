package com.cgi.eoss.ftep.search.providers.ceda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class CedaResultTemporal {
    @JsonProperty("start_time")
    private LocalDateTime startTime;
    @JsonProperty("end_time")
    private LocalDateTime endTime;
}
