package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.FtepServiceDockerBuildInfo;
import com.cgi.eoss.ftep.model.FtepServiceDockerBuildInfo.Status;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.rpc.BuildServiceParams;
import com.cgi.eoss.ftep.rpc.BuildServiceResponse;
import com.cgi.eoss.ftep.rpc.LocalServiceLauncher;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.cgi.eoss.ftep.services.DefaultFtepServices;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.MoreFiles;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@RestController
@BasePathAwareController
@RequestMapping("/services")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class ServicesApiExtension {

    @Value("${ftep.api.logs.graylogApiBuildLogQuery:dockerBuildFingerprint%3A@{fingerprint}}")
    private String dockerBuildLogQuery;

    private final GraylogClient graylogClient;
    private final ServiceDataService serviceDataService;
    private final FtepSecurityService ftepSecurityService;
    private final LocalServiceLauncher localServiceLauncher;

    @GetMapping("/defaults")
    public Resources<FtepService> getDefaultServices() {
        // Use the default service list, but retrieve updated objects from the database
        return new Resources<>(DefaultFtepServices.getDefaultServices().stream()
                .map(s -> serviceDataService.getByName(s.getName())).collect(Collectors.toList()));
    }

    @GetMapping("/export/available")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void exportAvailableServices(HttpServletResponse response) throws IOException {
        Path zipBase = Files.createTempDirectory("servicesexport");
        Path zipFile = Files.createTempFile("availableServices", ".zip");

        try {
            List<FtepService> services = serviceDataService.findAllAvailable();

            for (FtepService service : services) {
                LOG.info("Exporting service {} to {}", service.getName(), zipBase);
                Path serviceDir = Files.createDirectories(zipBase.resolve(service.getName()));
                exportServiceFiles(zipBase, service, serviceDir);
            }

            try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                Files.walkFileTree(zipBase, new Util.ZippingVisitor(zipBase, zipOut));
            }

            Util.serveFileDownload(response, new PathResource(zipFile));
        } finally {
            MoreFiles.deleteRecursively(zipBase);
            Files.delete(zipFile);
        }
    }

    @GetMapping("/export/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (hasPermission(#service, 'administration'))")
    public void exportAvailableServices(@ModelAttribute("serviceId") @Param("service") FtepService service, HttpServletResponse response) throws IOException {
        Path zipBase = Files.createTempDirectory("servicesexport");
        Path zipFile = Files.createTempFile(service.getName(), ".zip");

        try {
            LOG.info("Exporting service {} to {}", service.getName(), zipBase);
            Path serviceDir = Files.createDirectories(zipBase.resolve(service.getName()));

            exportServiceFiles(zipBase, service, serviceDir);

            try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                Files.walkFileTree(zipBase, new Util.ZippingVisitor(zipBase, zipOut));
            }

            Util.serveFileDownload(response, new PathResource(zipFile));
        } finally {
            MoreFiles.deleteRecursively(zipBase);
            Files.delete(zipFile);
        }
    }

    private void exportServiceFiles(Path zipBase, FtepService service, Path serviceDir) throws IOException {
        for (FtepServiceContextFile serviceContextFile : service.getContextFiles()) {
            LOG.debug("Exporting service {} context file {}", service.getName(), serviceContextFile.getFilename());
            Path fileOut = serviceDir.resolve(serviceContextFile.getFilename());
            Files.createDirectories(fileOut.getParent());
            Files.write(fileOut, serviceContextFile.getContent().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        Files.write(zipBase.resolve(service.getName() + ".yaml"), service.getServiceDescriptor().toYaml().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // DOCKER BUILD SERVICE FINGERPRINT - BEGIN ----------------------------------- //

    /**
     * <p>Provides information on the status of the service Docker build</p>
     */
    @GetMapping("/{serviceId}/buildStatus")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'read')")
    public ResponseEntity<BuildStatus> buildStatus(@ModelAttribute("serviceId") FtepService service) {
        String currentServiceFingerprint = serviceDataService.computeServiceFingerprint(service);
        boolean needsBuild = needsBuild(service, currentServiceFingerprint);
        FtepServiceDockerBuildInfo.Status status;
        String fingerprint;
        if (service.getDockerBuildInfo() == null) {
            status = FtepServiceDockerBuildInfo.Status.NOT_STARTED;
            fingerprint = null;
        } else {
            status = service.getDockerBuildInfo().getDockerBuildStatus();
            fingerprint = service.getDockerBuildInfo().getLastBuiltFingerprint();
        }
        BuildStatus buildStatus = new BuildStatus(needsBuild, status, fingerprint);
        return new ResponseEntity<>(buildStatus, HttpStatus.OK);
    }

    /**
     * <p>Builds the service docker image.</p>
     * <p>Build is launched asynchronously.</p>
     */
    @PostMapping("/{serviceId}/build")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'write')")
    public ResponseEntity build(@ModelAttribute("serviceId") FtepService service) {
        FtepServiceDockerBuildInfo dockerBuildInfo = service.getDockerBuildInfo();
        if (dockerBuildInfo != null && dockerBuildInfo.getDockerBuildStatus().equals(Status.IN_PROCESS)) {
            return new ResponseEntity<>("A build is already in process", HttpStatus.CONFLICT);
        } else if (dockerBuildInfo != null && dockerBuildInfo.getDockerBuildStatus().equals(Status.REQUESTED)) {
            return new ResponseEntity<>("A build has already been requested", HttpStatus.CONFLICT);
        } else {
            String currentServiceFingerprint = serviceDataService.computeServiceFingerprint(service);
            LOG.info("Building service via REST API: {}", service.getName());
            if (dockerBuildInfo == null) {
                dockerBuildInfo = new FtepServiceDockerBuildInfo();
                service.setDockerBuildInfo(dockerBuildInfo);
            }
            dockerBuildInfo.setDockerBuildStatus(Status.REQUESTED);
            serviceDataService.save(service);
            BuildServiceParams.Builder buildServiceParamsBuilder = BuildServiceParams.newBuilder()
                    .setUserId(ftepSecurityService.getCurrentUser().getName())
                    .setServiceId(String.valueOf(service.getId()))
                    .setServiceName(service.getName())
                    .setBuildFingerprint(currentServiceFingerprint);
            BuildServiceParams buildServiceParams = buildServiceParamsBuilder.build();
            buildService(service, buildServiceParams);
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        }
    }

    /**
     * <p>Stops the service docker image build.</p>
     */
    @PostMapping("/{serviceId}/stopBuild")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'write')")
    public ResponseEntity stopBuild(@ModelAttribute("serviceId") FtepService service) {
        FtepServiceDockerBuildInfo dockerBuildInfo = service.getDockerBuildInfo();
        if (dockerBuildInfo != null && (dockerBuildInfo.getDockerBuildStatus().equals(Status.REQUESTED) || dockerBuildInfo.getDockerBuildStatus().equals(Status.IN_PROCESS))) {
            dockerBuildInfo.setDockerBuildStatus(Status.CANCELLED);
            serviceDataService.save(service);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>("No ongoing build found", HttpStatus.CONFLICT);
    }

    /**
     * <p>Retrieve the Docker image build logs from Graylog</p>
     */
    @GetMapping(value = "/{serviceId}/buildLogs")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public ResponseEntity buildLogs(@ModelAttribute("serviceId") FtepService service, @RequestParam("fingerprint") String fingerprint) throws IOException {
        Map<String, String> parameters = ImmutableMap.<String, String>builder()
                .put("range", "0")
                .put("sort", "timestamp%3Aasc")
                .put("decorate", "false")
                .put("fields", "timestamp%2Cmessage")
                .put("query", StrSubstitutor.replace(dockerBuildLogQuery, ImmutableMap.of("fingerprint", fingerprint), "@{", "}"))
                .build();

        // Really simple HTML table presentation of the logs
        StringBuilder html = new StringBuilder(String.format("<html><head><title>Docker build logs: %s (%s)</title></head><body>", service.getName(), fingerprint));
        html.append("<table style=\"font-family:monospace;border-collapse:collapse;text-align:left;\">");
        graylogClient.loadMessages(parameters).forEach(msg -> {
            LOG.debug("Message: {}", msg.getMessage());
            html.append(String.format("<tr><td valign=\"top\" style=\"padding:1px 4px;\">%s</td><td valign=\"top\" style=\"padding:1px 4px;\">%s</td></tr>", msg.getTimestamp(), msg.getMessage().replace(System.lineSeparator(), "<br/>")));
        });
        html.append("</table></body></html>");

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html.toString());
    }

    private boolean needsBuild(FtepService ftepService, String currentServiceFingerprint) {
        // Presumably the order of these facts matter
        if (null == ftepService.getDockerBuildInfo()) {
            return true;
        }
        FtepServiceDockerBuildInfo dockerBuildInfo = ftepService.getDockerBuildInfo();
        if (dockerBuildInfo.getDockerBuildStatus() == Status.REQUESTED || dockerBuildInfo.getDockerBuildStatus() == Status.IN_PROCESS) {
            return false;
        }
        if (null == dockerBuildInfo.getLastBuiltFingerprint()) {
            return true;
        }
        return !currentServiceFingerprint.equals(dockerBuildInfo.getLastBuiltFingerprint());
    }

    private void buildService(FtepService ftepService, BuildServiceParams buildServiceParams) {
        serviceDataService.save(ftepService);
        BuildServiceObserver responseObserver = new BuildServiceObserver();
        localServiceLauncher.asyncBuildService(buildServiceParams, responseObserver);
    }

    @lombok.Value
    private static class BuildStatus {
        Boolean needsBuild;
        FtepServiceDockerBuildInfo.Status status;
        String serviceFingerprint;
    }

    private static class BuildServiceObserver implements StreamObserver<BuildServiceResponse> {
        public BuildServiceObserver() {
        }

        @Override
        public void onNext(BuildServiceResponse value) {
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onCompleted() {
        }
    }

    // DOCKER BUILD SERVICE FINGERPRINT - END ------------------------------------- //
}
