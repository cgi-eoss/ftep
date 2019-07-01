package com.cgi.eoss.ftep.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class FtepFileExternalReferences {
    private List<Job> jobs;
    private List<JobConfig> jobConfigs;
    private List<Databasket> databaskets;
}