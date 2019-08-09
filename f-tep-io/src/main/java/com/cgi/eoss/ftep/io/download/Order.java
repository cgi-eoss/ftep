package com.cgi.eoss.ftep.io.download;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {
    private Integer id;
    private String status; // enum: "queued", "processing", "pausing", "paused", "done", "cancelled"
    private String orderName;
    private Integer priority;
    private String destination;
    private String callback;
    private List<String> identifierList;
    private String processor;
    private String keycloakUuid;
    private String addDate;
    private String restoQuery;
    private String extra;
}
