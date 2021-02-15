package com.cgi.eoss.ftep.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtepServiceDockerBuildInfo {

    private String lastBuiltFingerprint;

    @Builder.Default
    private Status dockerBuildStatus = Status.NOT_STARTED;

    public enum Status {
        NOT_STARTED, REQUESTED, IN_PROCESS, COMPLETED, FAILED
    }
}
