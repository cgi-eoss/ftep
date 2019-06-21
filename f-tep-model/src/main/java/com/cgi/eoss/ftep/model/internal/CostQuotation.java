package com.cgi.eoss.ftep.model.internal;

import lombok.Data;

@Data
public class CostQuotation {
    public enum Recurrence {
        ONE_OFF, HOURLY, DAILY, MONTHLY;
    }

    private final int cost;
    private final Recurrence recurrence;
}