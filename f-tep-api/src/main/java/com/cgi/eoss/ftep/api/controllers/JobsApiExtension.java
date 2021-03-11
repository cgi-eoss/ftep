package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.costing.CostingService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.LocalServiceLauncher;
import com.cgi.eoss.ftep.rpc.StopServiceParams;
import com.cgi.eoss.ftep.rpc.StopServiceResponse;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.google.common.collect.ImmutableMap;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RestController
@BasePathAwareController
@RequestMapping("/jobs")
@Transactional
@Log4j2
public class JobsApiExtension {

    @Value("${ftep.api.logs.graylogApiQuery:zooId%3A@{zooId}%20AND%20userMessage%3A1}")
    private String dockerJobLogQuery;

    private final GraylogClient graylogClient;
    private final LocalServiceLauncher localServiceLauncher;
    private final FtepSecurityService ftepSecurityService;
    private final CatalogueService catalogueService;
    private final CostingService costingService;

    @Autowired
    public JobsApiExtension(GraylogClient graylogClient, LocalServiceLauncher localServiceLauncher, FtepSecurityService ftepSecurityService, CatalogueService catalogueService, CostingService costingService) {
        this.graylogClient = graylogClient;
        this.localServiceLauncher = localServiceLauncher;
        this.ftepSecurityService = ftepSecurityService;
        this.catalogueService = catalogueService;
        this.costingService = costingService;
    }

    @GetMapping(value = "/{jobId}/dl")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'read')")
    public void downloadOutputs(@ModelAttribute("jobId") Job job, HttpServletResponse response) throws IOException {
        Set<FtepFile> outputFiles = job.getOutputFiles();

        User user = ftepSecurityService.getCurrentUser();
        int estimatedCost = outputFiles.stream().mapToInt(costingService::estimateDownloadCost).sum();
        if (estimatedCost > user.getWallet().getBalance()) {
            response.setStatus(HttpStatus.PAYMENT_REQUIRED.value());
            String message = "Estimated download cost for all job outputs (" + estimatedCost + " coins) exceeds current wallet balance";
            response.getOutputStream().write(message.getBytes());
            response.flushBuffer();
            return;
        }
        // TODO Should estimated cost be "locked" in the wallet?

        Util.serveFileDownload(response, catalogueService.getAsZipResource(job.getExtId() + ".zip", outputFiles));

        outputFiles.forEach(file -> costingService.chargeForDownload(user.getWallet(), file));
    }

    @GetMapping("/{jobId}/logs")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'read')")
    @ResponseBody
    public List<GraylogClient.SimpleMessage> getJobContainerLogs(@ModelAttribute("jobId") Job job) throws IOException {
        Map<String, String> parameters = ImmutableMap.<String, String>builder()
                .put("range", "0")
                .put("sort", "timestamp%3Aasc")
                .put("decorate", "false")
                .put("fields", "timestamp%2Cmessage")
                .put("query", StrSubstitutor.replace(dockerJobLogQuery, ImmutableMap.of("zooId", job.getExtId()), "@{", "}"))
                .build();

        return graylogClient.loadMessages(parameters);
    }

    @PostMapping("/{jobId}/terminate")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'write')")
    public ResponseEntity terminateJob(@ModelAttribute("jobId") Job job) throws InterruptedException {
        StopServiceParams stopServiceParams = StopServiceParams.newBuilder().setJob(GrpcUtil.toRpcJob(job)).build();
        final CountDownLatch latch = new CountDownLatch(1);
        JobTerminateObserver responseObserver = new JobTerminateObserver(latch);
        localServiceLauncher.asyncStopJob(stopServiceParams, responseObserver);
        latch.await(1, TimeUnit.MINUTES);
        return ResponseEntity.noContent().build();
    }

    private static final class JobTerminateObserver implements StreamObserver<StopServiceResponse> {
        private final CountDownLatch latch;

        JobTerminateObserver(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNext(StopServiceResponse value) {
            LOG.debug("Received StopServiceResponse: {}", value);
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
            LOG.error("Failed to stop service via REST API", t);
        }

        @Override
        public void onCompleted() {
            // No-op, the user has long stopped listening here
        }
    }
}
