package com.cgi.eoss.ftep.metrics;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.JobConfigDataService;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MetricsConfig.class})
@TestPropertySource("classpath:test-metrics.properties")
@Transactional
public class FtepServiceExecutionMetricsTest {

    @Autowired
    private PublicMetrics ftepServiceExecutionMetrics;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private JobConfigDataService jobConfigDataService;

    @Autowired
    private JobDataService jobDataService;

    private final List<User> users = Lists.newArrayList(
            new User("user-1"),
            new User("user-2")
    );

    private final List<FtepService> services = Lists.newArrayList(
            newService("service-1", users.get(0), FtepService.Type.PROCESSOR),
            newService("service-2", users.get(0), FtepService.Type.BULK_PROCESSOR),
            newService("service-3", users.get(0), FtepService.Type.APPLICATION)
    );

    private final List<JobConfig> jobConfigs = Lists.newArrayList(
            new JobConfig(users.get(0), services.get(0)),
            new JobConfig(users.get(0), services.get(1)),
            new JobConfig(users.get(0), services.get(2))
    );

    private final List<Job> jobs = Lists.newArrayList(
            newJob(jobConfigs.get(0), UUID.randomUUID().toString(), users.get(0), Job.Status.CREATED, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(0), UUID.randomUUID().toString(), users.get(1), Job.Status.COMPLETED, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(0), UUID.randomUUID().toString(), users.get(0), Job.Status.COMPLETED, LocalDateTime.now(ZoneOffset.UTC).minusDays(31)),
            newJob(jobConfigs.get(0), UUID.randomUUID().toString(), users.get(0), Job.Status.RUNNING, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(0), UUID.randomUUID().toString(), users.get(0), Job.Status.CANCELLED, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(0), UUID.randomUUID().toString(), users.get(0), Job.Status.ERROR, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(0), UUID.randomUUID().toString(), users.get(0), Job.Status.ERROR, LocalDateTime.now(ZoneOffset.UTC).minusDays(31)),
            newJob(jobConfigs.get(0), UUID.randomUUID().toString(), users.get(0), Job.Status.ERROR, LocalDateTime.now(ZoneOffset.UTC).minusDays(1)),

            newJob(jobConfigs.get(1), UUID.randomUUID().toString(), users.get(0), Job.Status.CREATED, LocalDateTime.now(ZoneOffset.UTC).minusDays(31)),
            newJob(jobConfigs.get(1), UUID.randomUUID().toString(), users.get(0), Job.Status.COMPLETED, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(1), UUID.randomUUID().toString(), users.get(0), Job.Status.COMPLETED, LocalDateTime.now(ZoneOffset.UTC).minusDays(31)),
            newJob(jobConfigs.get(1), UUID.randomUUID().toString(), users.get(0), Job.Status.RUNNING, LocalDateTime.now(ZoneOffset.UTC).minusDays(31)),
            newJob(jobConfigs.get(1), UUID.randomUUID().toString(), users.get(0), Job.Status.CANCELLED, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(1), UUID.randomUUID().toString(), users.get(0), Job.Status.ERROR, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(1), UUID.randomUUID().toString(), users.get(0), Job.Status.ERROR, LocalDateTime.now(ZoneOffset.UTC).minusDays(31)),
            newJob(jobConfigs.get(1), UUID.randomUUID().toString(), users.get(0), Job.Status.ERROR, LocalDateTime.now(ZoneOffset.UTC).minusDays(1)),

            newJob(jobConfigs.get(2), UUID.randomUUID().toString(), users.get(0), Job.Status.CREATED, LocalDateTime.now(ZoneOffset.UTC).minusDays(1)),
            newJob(jobConfigs.get(2), UUID.randomUUID().toString(), users.get(0), Job.Status.COMPLETED, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(2), UUID.randomUUID().toString(), users.get(0), Job.Status.COMPLETED, LocalDateTime.now(ZoneOffset.UTC).minusDays(1)),
            newJob(jobConfigs.get(2), UUID.randomUUID().toString(), users.get(0), Job.Status.RUNNING, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(2), UUID.randomUUID().toString(), users.get(0), Job.Status.CANCELLED, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(2), UUID.randomUUID().toString(), users.get(0), Job.Status.ERROR, LocalDateTime.now(ZoneOffset.UTC).minusDays(91)),
            newJob(jobConfigs.get(2), UUID.randomUUID().toString(), users.get(0), Job.Status.ERROR, LocalDateTime.now(ZoneOffset.UTC).minusDays(31)),
            newJob(jobConfigs.get(2), UUID.randomUUID().toString(), users.get(0), Job.Status.ERROR, LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
    );

    @Before
    public void setUp() throws Exception {
        userDataService.save(users);
        serviceDataService.save(services);
        jobConfigDataService.save(jobConfigs);
        jobDataService.save(jobs);
    }

    @Test
    public void metrics() throws Exception {
        Map<String, Integer> metrics = ftepServiceExecutionMetrics.metrics().stream().collect(toMap(Metric::getName, m -> m.getValue().intValue()));

        assertThat(metrics.get("ftep.jobs.application.30d.cancelled"), is(0));
        assertThat(metrics.get("ftep.jobs.application.30d.completed"), is(1));
        assertThat(metrics.get("ftep.jobs.application.30d.created"), is(1));
        assertThat(metrics.get("ftep.jobs.application.30d.error"), is(1));
        assertThat(metrics.get("ftep.jobs.application.30d.running"), is(0));
        assertThat(metrics.get("ftep.jobs.application.90d.cancelled"), is(0));
        assertThat(metrics.get("ftep.jobs.application.90d.completed"), is(1));
        assertThat(metrics.get("ftep.jobs.application.90d.created"), is(1));
        assertThat(metrics.get("ftep.jobs.application.90d.error"), is(2));
        assertThat(metrics.get("ftep.jobs.application.90d.running"), is(0));
        assertThat(metrics.get("ftep.jobs.application.cancelled"), is(1));
        assertThat(metrics.get("ftep.jobs.application.completed"), is(2));
        assertThat(metrics.get("ftep.jobs.application.created"), is(1));
        assertThat(metrics.get("ftep.jobs.application.error"), is(3));
        assertThat(metrics.get("ftep.jobs.application.running"), is(1));

        assertThat(metrics.get("ftep.jobs.bulk_processor.30d.cancelled"), is(0));
        assertThat(metrics.get("ftep.jobs.bulk_processor.30d.completed"), is(0));
        assertThat(metrics.get("ftep.jobs.bulk_processor.30d.created"), is(0));
        assertThat(metrics.get("ftep.jobs.bulk_processor.30d.error"), is(1));
        assertThat(metrics.get("ftep.jobs.bulk_processor.30d.running"), is(0));
        assertThat(metrics.get("ftep.jobs.bulk_processor.90d.cancelled"), is(0));
        assertThat(metrics.get("ftep.jobs.bulk_processor.90d.completed"), is(1));
        assertThat(metrics.get("ftep.jobs.bulk_processor.90d.created"), is(1));
        assertThat(metrics.get("ftep.jobs.bulk_processor.90d.error"), is(2));
        assertThat(metrics.get("ftep.jobs.bulk_processor.90d.running"), is(1));
        assertThat(metrics.get("ftep.jobs.bulk_processor.cancelled"), is(1));
        assertThat(metrics.get("ftep.jobs.bulk_processor.completed"), is(2));
        assertThat(metrics.get("ftep.jobs.bulk_processor.created"), is(1));
        assertThat(metrics.get("ftep.jobs.bulk_processor.error"), is(3));
        assertThat(metrics.get("ftep.jobs.bulk_processor.running"), is(1));

        assertThat(metrics.get("ftep.jobs.processor.30d.cancelled"), is(0));
        assertThat(metrics.get("ftep.jobs.processor.30d.completed"), is(0));
        assertThat(metrics.get("ftep.jobs.processor.30d.created"), is(0));
        assertThat(metrics.get("ftep.jobs.processor.30d.error"), is(1));
        assertThat(metrics.get("ftep.jobs.processor.30d.running"), is(0));
        assertThat(metrics.get("ftep.jobs.processor.90d.cancelled"), is(0));
        assertThat(metrics.get("ftep.jobs.processor.90d.completed"), is(1));
        assertThat(metrics.get("ftep.jobs.processor.90d.created"), is(0));
        assertThat(metrics.get("ftep.jobs.processor.90d.error"), is(2));
        assertThat(metrics.get("ftep.jobs.processor.90d.running"), is(0));
        assertThat(metrics.get("ftep.jobs.processor.cancelled"), is(1));
        assertThat(metrics.get("ftep.jobs.processor.completed"), is(2));
        assertThat(metrics.get("ftep.jobs.processor.created"), is(1));
        assertThat(metrics.get("ftep.jobs.processor.error"), is(3));
        assertThat(metrics.get("ftep.jobs.processor.running"), is(1));

        assertThat(metrics.get("ftep.jobs.total.30d.cancelled"), is(0));
        assertThat(metrics.get("ftep.jobs.total.30d.completed"), is(1));
        assertThat(metrics.get("ftep.jobs.total.30d.created"), is(1));
        assertThat(metrics.get("ftep.jobs.total.30d.error"), is(3));
        assertThat(metrics.get("ftep.jobs.total.30d.running"), is(0));
        assertThat(metrics.get("ftep.jobs.total.90d.cancelled"), is(0));
        assertThat(metrics.get("ftep.jobs.total.90d.completed"), is(3));
        assertThat(metrics.get("ftep.jobs.total.90d.created"), is(2));
        assertThat(metrics.get("ftep.jobs.total.90d.error"), is(6));
        assertThat(metrics.get("ftep.jobs.total.90d.running"), is(1));
        assertThat(metrics.get("ftep.jobs.total.cancelled"), is(3));
        assertThat(metrics.get("ftep.jobs.total.completed"), is(6));
        assertThat(metrics.get("ftep.jobs.total.created"), is(3));
        assertThat(metrics.get("ftep.jobs.total.error"), is(9));
        assertThat(metrics.get("ftep.jobs.total.running"), is(3));

        assertThat(metrics.get("ftep.jobs.unique-users"), is(2));
        assertThat(metrics.get("ftep.jobs.unique-users.90d"), is(1));
        assertThat(metrics.get("ftep.jobs.unique-users.30d"), is(1));
    }

    private FtepService newService(String name, User user, FtepService.Type type) {
        FtepService service = new FtepService(name, user, name);
        service.setType(type);
        return service;
    }

    private Job newJob(JobConfig jobConfig, String uuid, User user, Job.Status status, LocalDateTime startTime) {
        Job job = new Job(jobConfig, uuid, user);
        job.setStatus(status);
        job.setStartTime(startTime);
        return job;
    }
}
