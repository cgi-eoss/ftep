package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.SystematicProcessing;
import com.cgi.eoss.ftep.persistence.dao.JobDao;
import com.cgi.eoss.ftep.persistence.dao.JobConfigDao;
import com.cgi.eoss.ftep.persistence.dao.SystematicProcessingDao;
import com.cgi.eoss.ftep.rpc.FtepJobResponse;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.LocalServiceLauncher;
import com.cgi.eoss.ftep.security.FtepSecurityService;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>A {@link RepositoryRestController} for interacting with {@link JobConfig}s. Offers additional functionality over
 * the standard CRUD-style {@link JobConfigsApi}.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/jobConfigs")
@Log4j2
public class JobConfigsApiExtension {

    private final FtepSecurityService ftepSecurityService;
    private final LocalServiceLauncher localServiceLauncher;
    private final JobDao jobRepository;
    private final JobConfigDao jobConfigDao;
    private final SystematicProcessingDao systematicProcessingDao;

    @Autowired
    public JobConfigsApiExtension(FtepSecurityService ftepSecurityService, LocalServiceLauncher localServiceLauncher, JobDao jobRepository, JobConfigDao jobConfigDao, SystematicProcessingDao systematicProcessingDao) {
        this.ftepSecurityService = ftepSecurityService;
        this.localServiceLauncher = localServiceLauncher;
        this.jobRepository = jobRepository;
        this.jobConfigDao = jobConfigDao;
        this.systematicProcessingDao = systematicProcessingDao;
    }

    /**
     * <p>Provides a direct interface to the service orchestrator, allowing users to launch job configurations without going via WPS.</p>
     * <p>Service are launched asynchronously; the gRPC response is discarded.</p>
     *
     * @throws InterruptedException
     */
    @PostMapping("/{jobConfigId}/launch")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'read')")
    public ResponseEntity<Resource<Job>> launch(@ModelAttribute("jobConfigId") JobConfig jobConfig) throws InterruptedException {
        FtepServiceParams.Builder serviceParamsBuilder = FtepServiceParams.newBuilder()
                .setJobId(UUID.randomUUID().toString())
                .setUserId(ftepSecurityService.getCurrentUser().getName())
                .setServiceId(jobConfig.getService().getName())
                .addAllInputs(GrpcUtil.mapToParams(jobConfig.getInputs()));

        if (!Strings.isNullOrEmpty(jobConfig.getLabel())) {
            serviceParamsBuilder.setJobConfigLabel(jobConfig.getLabel());
        }

        Optional.ofNullable(jobConfig.getParent())
                .ifPresent(parent -> serviceParamsBuilder.setJobParent(String.valueOf(parent.getId())));

        FtepServiceParams serviceParams = serviceParamsBuilder.build();

        LOG.info("Launching service via REST API: {}", serviceParams);

        final CountDownLatch latch = new CountDownLatch(1);
        JobLaunchObserver responseObserver = new JobLaunchObserver(latch);
        localServiceLauncher.asyncLaunchService(serviceParams, responseObserver);

        // Block until the latch counts down (i.e. one message from the server
        latch.await(1, TimeUnit.MINUTES);
        Job job = jobRepository.getOne(responseObserver.getIntJobId());
        return ResponseEntity.accepted().body(new Resource<>(job));
    }

    /**
     * <p>Provides a direct interface to the service orchestrator, allowing users to launch job configurations without going via WPS.</p>
     * <p>Service are launched asynchronously; the gRPC response is discarded.</p>
     *
     * @throws InterruptedException
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws JsonProcessingException
     * @throws IOException
     */
    @PostMapping("/launchSystematic")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#jobConfigTemplate.id == null) or hasPermission(#jobConfigTemplate, 'read')")
    public ResponseEntity<Void> launchSystematic(HttpServletRequest request, @RequestBody JobConfig jobConfigTemplate) throws InterruptedException, JsonParseException, JsonMappingException, JsonProcessingException, IOException {
        LOG.debug("Received new request for systematic processing");

        // Save the job config
        ftepSecurityService.updateOwnerWithCurrentUser(jobConfigTemplate);
        jobConfigDao.save(jobConfigTemplate);

        // Create "master" job
        Job parentJob = new Job(jobConfigTemplate, UUID.randomUUID().toString(), ftepSecurityService.getCurrentUser());
        jobRepository.save(parentJob);

        // Save the systematic processing
        SystematicProcessing systematicProcessing = new SystematicProcessing();
        systematicProcessing.setParentJob(parentJob);
        ftepSecurityService.updateOwnerWithCurrentUser(systematicProcessing);
        Map<String, String[]> requestParameters = request.getParameterMap();
        ListMultimap<String, String> searchParameters = ArrayListMultimap.create();
        for (Map.Entry<String, String[]> entry : requestParameters.entrySet()) {
            searchParameters.putAll(entry.getKey(), Arrays.asList(entry.getValue()));
        }

        // Put the necessary parameters for systematic processing
        searchParameters.put("sortOrder", "ascending");
        searchParameters.put("sortParam", "updated");

        List<String> dateStartParam = searchParameters.get("productDateStart");

        String dateStart;

        if (dateStartParam != null && dateStartParam.size() > 0) {
            dateStart = dateStartParam.get(0);
        } else {
            dateStart = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
            searchParameters.put("productDateStart", dateStart);
        }

        systematicProcessing.setLastUpdated(ZonedDateTime.parse(dateStart).toLocalDateTime());

        systematicProcessing.setSearchParameters(searchParameters);
        systematicProcessingDao.save(systematicProcessing);
        LOG.info("Systematic processing saved");

        return ResponseEntity.accepted().build();
    }

    private static final class JobLaunchObserver implements StreamObserver<FtepJobResponse> {
        private final CountDownLatch latch;

        @Getter
        private long intJobId;

        public JobLaunchObserver(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNext(FtepJobResponse value) {
            if (latch.getCount() != 0) {
                this.intJobId = Long.parseLong(value.getJob().getIntJobId());
                LOG.info("Received job ID: {}", this.intJobId);
                latch.countDown();
            }
        }

        @Override
        public void onError(Throwable t) {
            LOG.error("Failed to launch service via REST API", t);
        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }
}
