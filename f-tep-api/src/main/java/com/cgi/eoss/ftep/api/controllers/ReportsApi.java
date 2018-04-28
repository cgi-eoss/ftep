package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.orchestrator.service.FtepFileRegistrar;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Functionality for accessing the F-TEP unifying search facade.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/reports")
@Log4j2
public class ReportsApi {

    private final JobDataService jobDataService;
    private final FtepFileRegistrar ftepFileRegistrar;

    @Autowired
    public ReportsApi(JobDataService jobDataService, FtepFileRegistrar ftepFileRegistrar) {
        this.jobDataService = jobDataService;
        this.ftepFileRegistrar = ftepFileRegistrar;
    }

    @PostMapping("/updateJobFiles")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public ResponseEntity updateJobFiles() {
        jobDataService.getAllIds().forEach(this::updateJobFiles);
        LOG.info("Finished indexing job input files");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/updateJobFiles/{jobId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void updateJobFiles(@PathVariable("jobId") long jobId) {
        Job job = jobDataService.getById(jobId);
        LOG.info("Updating job input files for job {}", job.getId());
        ftepFileRegistrar.registerInputFiles(job);
    }

    @GetMapping("/dataUsage/{months}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public DataUsageReport getDataUsageReport(@PathVariable("months") int months) {
        DataUsageReport.DataUsageReportBuilder reportBuilder = DataUsageReport.builder();
        LocalDateTime start = LocalDateTime.now();

        for (int i = months; i >= 0; i--) {
            YearMonth current = YearMonth.from(start.minusMonths(i));

            List<Job> jobs = jobDataService.findByStartIn(current);

            DataUsage.DataUsageBuilder usageBuilder = DataUsage.builder();

            List<JobDataUsage> jobDataUsages = jobs.stream()
                    .map(this::getJobDataUsage)
                    .collect(Collectors.toList());

            jobDataUsages.forEach(jdu -> {
                jdu.getFiles().forEach(usageBuilder::file);
                usageBuilder.job(jdu);
            });
            usageBuilder.totalSize(jobDataUsages.stream().mapToLong(JobDataUsage::getTotalSize).sum());

            reportBuilder.report(current, usageBuilder.build());
        }

        return reportBuilder.build();
    }

    private JobDataUsage getJobDataUsage(Job job) {
        JobDataUsage.JobDataUsageBuilder builder = JobDataUsage.builder();

        builder.id(job.getId());

        job.getConfig().getInputFiles().stream().map(this::getFileUsage).forEach(builder::file);

        builder.totalSize(job.getConfig().getInputFiles().stream()
                .mapToLong(f -> Optional.ofNullable(f.getFilesize()).orElse(0L))
                .sum());

        return builder.build();
    }

    private FileUsage getFileUsage(FtepFile ftepFile) {
        return FileUsage.builder()
                .uri(ftepFile.getUri().toString())
                .size(Optional.ofNullable(ftepFile.getFilesize()).orElse(0L))
                .build();
    }

    @Data
    @Builder
    private static final class DataUsageReport {
        @Singular
        private Map<YearMonth, DataUsage> reports;
    }

    @Data
    @Builder
    private static final class DataUsage {
        @Singular
        private Set<FileUsage> files;
        @Singular
        private List<JobDataUsage> jobs;
        private long totalSize;
    }

    @Data
    @Builder
    private static final class JobDataUsage {
        private long id;
        @Singular
        private Set<FileUsage> files;
        private long totalSize;
    }

    @Data
    @Builder
    private static final class FileUsage {
        private String uri;
        private long size;
    }

}
