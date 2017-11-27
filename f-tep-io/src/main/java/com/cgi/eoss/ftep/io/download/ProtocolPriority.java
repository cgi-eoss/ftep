package com.cgi.eoss.ftep.io.download;

import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Optional;

@Data
@Builder
public class ProtocolPriority {
    private int overallPriority;
    @Singular
    private ImmutableMap<String, Integer> protocolPriorities;

    int get(String protocol) {
        return Optional.ofNullable(protocolPriorities.get(protocol)).orElse(overallPriority);
    }
}
