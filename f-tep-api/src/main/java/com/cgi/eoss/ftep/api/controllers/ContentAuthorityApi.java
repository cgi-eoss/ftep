package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.PublishingRequest;
import com.cgi.eoss.ftep.orchestrator.zoo.ZooManagerClient;
import com.cgi.eoss.ftep.persistence.service.PublishingRequestDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.cgi.eoss.ftep.services.DefaultFtepServices;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Functionality for users with the CONTENT_AUTHORITY Role.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/contentAuthority")
@Log4j2
public class ContentAuthorityApi {

    private final FtepSecurityService ftepSecurityService;
    private final ZooManagerClient zooManagerClient;
    private final PublishingRequestDataService publishingRequestsDataService;
    private final ServiceDataService serviceDataService;

    @Autowired
    public ContentAuthorityApi(FtepSecurityService ftepSecurityService, ZooManagerClient zooManagerClient, PublishingRequestDataService publishingRequestsDataService, ServiceDataService serviceDataService) {
        this.ftepSecurityService = ftepSecurityService;
        this.zooManagerClient = zooManagerClient;
        this.publishingRequestsDataService = publishingRequestsDataService;
        this.serviceDataService = serviceDataService;
    }

    @PostMapping("/services/restoreDefaults")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void restoreDefaultServices() {
        Set<FtepService> defaultServices = DefaultFtepServices.getDefaultServices();

        for (FtepService service : defaultServices) {
            LOG.info("Restoring default service: {}", service.getName());

            // If the service already exists, synchronise the IDs (and associated file IDs) to avoid constraint errors
            Optional.ofNullable(serviceDataService.getByName(service.getName())).ifPresent((FtepService existing) -> {
                service.setId(existing.getId());
                service.getContextFiles().forEach(newFile ->
                        existing.getContextFiles().stream()
                                .filter(existingFile -> existingFile.getFilename().equals(newFile.getFilename()))
                                .findFirst()
                                .ifPresent(existingFile -> newFile.setId(existingFile.getId())));
            });

            service.setOwner(ftepSecurityService.refreshPersistentUser(service.getOwner()));
            serviceDataService.save(service);
            publishService(service);
        }
    }

    @PostMapping("/services/wps/syncAllPublic")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void wpsSyncAllPublic() {
        // Find all Status.AVAILABLE, then filter for those visible to PUBLIC
        List<FtepService> publicServices = serviceDataService.findAllAvailable().stream()
                .filter(s -> ftepSecurityService.isPublic(FtepService.class, s.getId()))
                .collect(Collectors.toList());
        zooManagerClient.updateActiveZooServices(publicServices);
    }

    @PostMapping("/services/publish/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void publishService(@ModelAttribute("serviceId") FtepService service) {
        service.setStatus(FtepService.Status.AVAILABLE);
        serviceDataService.save(service);

        ftepSecurityService.publish(FtepService.class, service.getId());
        publishingRequestsDataService.findRequestsForPublishing(service).forEach(request -> {
            request.setStatus(PublishingRequest.Status.GRANTED);
            publishingRequestsDataService.save(request);
        });
    }

    @PostMapping("/services/unpublish/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void unpublishService(@ModelAttribute("serviceId") FtepService service) {
        ftepSecurityService.unpublish(FtepService.class, service.getId());
    }

}
