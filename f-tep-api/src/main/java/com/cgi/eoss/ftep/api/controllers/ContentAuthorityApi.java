package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.internal.CompleteFtepService;
import com.cgi.eoss.ftep.orchestrator.zoo.ZooManagerClient;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.services.DefaultFtepServices;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Functionality for users with the CONTENT_AUTHORITY Role.</p>
 */
@RestController
@RequestMapping("/contentAuthority")
@Log4j2
public class ContentAuthorityApi {

    private final ServiceDataService serviceDataService;
    private final FtepSecurityService ftepSecurityService;
    private final ZooManagerClient zooManagerClient;

    @Autowired
    public ContentAuthorityApi(ServiceDataService serviceDataService, FtepSecurityService ftepSecurityService, ZooManagerClient zooManagerClient) {
        this.serviceDataService = serviceDataService;
        this.ftepSecurityService = ftepSecurityService;
        this.zooManagerClient = zooManagerClient;
    }

    @PostMapping("/services/restoreDefaults")
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    public void restoreDefaultServices() {
        Set<CompleteFtepService> defaultServices = DefaultFtepServices.getDefaultServices();

        for (CompleteFtepService service : defaultServices) {
            LOG.info("Restoring default service: {}", service.getService().getName());
            serviceDataService.save(service);
            publishService(service.getService());
        }
    }

    @PostMapping("/services/wps/syncAllPublic")
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    public void wpsSyncAllPublic() {
        // Find all ServiceStatus.AVAILABLE, then filter for those visible to PUBLIC
        List<FtepService> publicServices = serviceDataService.findAllAvailable().stream()
                .filter(s -> ftepSecurityService.isPublic(FtepService.class, s.getId()))
                .collect(Collectors.toList());
        zooManagerClient.updateActiveZooServices(publicServices);
    }

    @PostMapping("/services/publish/{serviceId}")
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    public void publishService(@ModelAttribute("serviceId") FtepService service) {
        ftepSecurityService.publish(FtepService.class, service.getId());
    }

    @PostMapping("/services/unpublish/{serviceId}")
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    public void unpublishService(@ModelAttribute("serviceId") FtepService service) {
        ftepSecurityService.unpublish(FtepService.class, service.getId());
    }

}
