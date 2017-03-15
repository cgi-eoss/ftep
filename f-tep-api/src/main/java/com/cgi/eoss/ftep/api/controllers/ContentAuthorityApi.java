package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.internal.CompleteFtepService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.services.DefaultFtepServices;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * <p>Functionality for users with the CONTENT_AUTHORITY Role.</p>
 */
@RestController
@RequestMapping("/contentAuthority")
@Log4j2
public class ContentAuthorityApi {

    private final ServiceDataService serviceDataService;
    private final FtepSecurityService ftepSecurityService;

    @Autowired
    public ContentAuthorityApi(ServiceDataService serviceDataService, FtepSecurityService ftepSecurityService) {
        this.serviceDataService = serviceDataService;
        this.ftepSecurityService = ftepSecurityService;
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

    @PostMapping("/services/{serviceId}")
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN')")
    public void publishService(@ModelAttribute("serviceId") FtepService service) {
        ftepSecurityService.publish(FtepService.class, service.getId());
    }

}
