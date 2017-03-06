package com.cgi.eoss.ftep.api.projections;

import org.springframework.beans.factory.annotation.Value;

public interface ShortProject extends EmbeddedId {
    String getName();
    String getDescription();
    ShortUser getOwner();
    @Value("#{target.databaskets.size()}")
    Integer getDatabasketsCount();
    @Value("#{target.jobConfigs.size()}")
    Integer getJobConfigsCount();
}
