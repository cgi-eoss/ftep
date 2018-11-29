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

import com.google.common.io.MoreFiles;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@RestController
@BasePathAwareController
@RequestMapping("/services")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class ServicesApiExtension {
    @Data
    public class BuildStatus {
        private final Boolean needsBuild;
        private final FtepServiceDockerBuildInfo.Status status;
    }

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
    public void exportAvailableServices(@ModelAttribute("serviceId") @P("service") FtepService service, HttpServletResponse response) throws IOException {
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
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public ResponseEntity<BuildStatus> buildStatus(@ModelAttribute("serviceId") FtepService service) {
        String currentServiceFingerprint = serviceDataService.computeServiceFingerprint(service);
        boolean needsBuild = needsBuild(service, currentServiceFingerprint);
        FtepServiceDockerBuildInfo.Status status;
        if (service.getDockerBuildInfo() == null) {
            status = FtepServiceDockerBuildInfo.Status.NOT_STARTED;
        } else {
            status = service.getDockerBuildInfo().getDockerBuildStatus();
        }
        BuildStatus buildStatus = new BuildStatus(needsBuild, status);
        return new ResponseEntity<>(buildStatus, HttpStatus.OK);
    }

    private boolean needsBuild(FtepService ftepService, String currentServiceFingerprint) {
        // Presumably the order of these facts matter
        if (null == ftepService.getDockerBuildInfo()) {
            return true;
        }
        if (ftepService.getDockerBuildInfo().getDockerBuildStatus() == FtepServiceDockerBuildInfo.Status.ONGOING) {
            return false;
        }
        if (null == ftepService.getDockerBuildInfo().getLastBuiltFingerprint()) {
            return true;
        }
        return !currentServiceFingerprint.equals(ftepService.getDockerBuildInfo().getLastBuiltFingerprint());
    }

    /**
     * <p>Builds the service docker image.</p>
     * <p>Build is launched asynchronously.</p>
     */
    @PostMapping("/{serviceId}/build")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public ResponseEntity build(@ModelAttribute("serviceId") FtepService service) {
        FtepServiceDockerBuildInfo dockerBuildInfo = service.getDockerBuildInfo();
        if (dockerBuildInfo != null && dockerBuildInfo.getDockerBuildStatus().equals(FtepServiceDockerBuildInfo.Status.ONGOING)) {
            return new ResponseEntity<>("A build is already ongoing", HttpStatus.CONFLICT);
        } else {
            String currentServiceFingerprint = serviceDataService.computeServiceFingerprint(service);
            if (needsBuild(service, currentServiceFingerprint)) {
                LOG.info("Building service via REST API: {}", service.getName());
                if (dockerBuildInfo == null) {
                    dockerBuildInfo = new FtepServiceDockerBuildInfo();
                    service.setDockerBuildInfo(dockerBuildInfo);
                }
                dockerBuildInfo.setDockerBuildStatus(Status.ONGOING);
                serviceDataService.save(service);
                BuildServiceParams.Builder buildServiceParamsBuilder = BuildServiceParams.newBuilder()
                    .setUserId(ftepSecurityService.getCurrentUser().getName())
                    .setServiceId(String.valueOf(service.getId()))
                    .setBuildFingerprint(currentServiceFingerprint);
                BuildServiceParams buildServiceParams = buildServiceParamsBuilder.build();
                buildService(service, buildServiceParams);
                return new ResponseEntity<>(HttpStatus.ACCEPTED);
            } else {
                return new ResponseEntity<>(HttpStatus.OK);
            }
        }
    }

    private void buildService(FtepService ftepService, BuildServiceParams buildServiceParams) {
        serviceDataService.save(ftepService);
        BuildServiceObserver responseObserver = new BuildServiceObserver();
        localServiceLauncher.asyncBuildService(buildServiceParams, responseObserver);
    }

    public class BuildServiceObserver implements StreamObserver<BuildServiceResponse> {
        public BuildServiceObserver() {}

        @Override
        public void onNext(BuildServiceResponse value) {}

        @Override
        public void onError(Throwable t) {}

        @Override
        public void onCompleted() {}
    }

    // DOCKER BUILD SERVICE FINGERPRINT - END ------------------------------------- //
}
