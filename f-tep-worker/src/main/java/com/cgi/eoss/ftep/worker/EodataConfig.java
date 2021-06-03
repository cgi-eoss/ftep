package com.cgi.eoss.ftep.worker;

import lombok.Data;

@Data
public class EodataConfig {
    private final String eodataMountPath;
    private final String eodataMountVolume;
}
