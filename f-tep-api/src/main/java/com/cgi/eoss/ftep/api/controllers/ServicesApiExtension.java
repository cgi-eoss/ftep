package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.services.DefaultFtepServices;
import com.google.common.io.MoreFiles;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resources;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

    private final ServiceDataService serviceDataService;

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
}
