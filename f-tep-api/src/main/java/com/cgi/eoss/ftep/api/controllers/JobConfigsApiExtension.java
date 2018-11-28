package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.persistence.dao.JobDao;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.FtepServiceResponse;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.LocalServiceLauncher;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.google.common.base.Strings;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Autowired
    public JobConfigsApiExtension(FtepSecurityService ftepSecurityService, LocalServiceLauncher localServiceLauncher, JobDao jobRepository) {
        this.ftepSecurityService = ftepSecurityService;
        this.localServiceLauncher = localServiceLauncher;
        this.jobRepository = jobRepository;
    }

    /**
     * <p>Provides a direct interface to the service orchestrator, allowing users to launch job configurations without going via WPS.</p>
     * <p>Service are launched asynchronously; the gRPC response is discarded.</p>
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

    private static final class JobLaunchObserver implements StreamObserver<FtepServiceResponse> {
        private final CountDownLatch latch;
        @Getter
        private long intJobId;

        JobLaunchObserver(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNext(FtepServiceResponse value) {
            if (value.getPayloadCase() == FtepServiceResponse.PayloadCase.JOB) {
                this.intJobId = Long.parseLong(value.getJob().getIntJobId());
                LOG.info("Received job ID: {}", this.intJobId);
            }
            latch.countDown();
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
