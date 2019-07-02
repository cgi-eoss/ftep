package com.cgi.eoss.ftep.io.download;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItem {
    private String addDate;
    private String extraInfo;
    private Integer id;
    private String modDate;
    private String name;
    private String newIdentifier;
    private Order order;
    private String product;
    private String result;
    private String status; // enum: "created", "ready_to_be_consumed_next", "consumed", "processing", "downloading", "not_found", "not_valid", "done", "removed_from_cache", "removing_from_cache", "scheduled_for_deletion", "failed"
}
