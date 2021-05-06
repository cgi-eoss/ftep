package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.PublishingRequest;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.orchestrator.zoo.ZooManagerClient;
import com.cgi.eoss.ftep.persistence.service.GroupDataService;
import com.cgi.eoss.ftep.persistence.service.PublishingRequestDataService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.cgi.eoss.ftep.security.FtepPermission;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.cgi.eoss.ftep.services.DefaultFtepServices;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final UserDataService userDataService;
    private final GroupDataService groupDataService;

    @Autowired
    public ContentAuthorityApi(FtepSecurityService ftepSecurityService, ZooManagerClient zooManagerClient, PublishingRequestDataService publishingRequestsDataService, ServiceDataService serviceDataService, UserDataService userDataService, GroupDataService groupDataService) {
        this.ftepSecurityService = ftepSecurityService;
        this.zooManagerClient = zooManagerClient;
        this.publishingRequestsDataService = publishingRequestsDataService;
        this.serviceDataService = serviceDataService;
        this.userDataService = userDataService;
        this.groupDataService = groupDataService;
    }

    @PostMapping("/services/restoreDefaults")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void restoreDefaultServices() {
        Set<FtepService> defaultServices = DefaultFtepServices.getDefaultServices();

        for (FtepService service : defaultServices) {
            resetService(service);
        }
    }

    @PostMapping("/services/restoreDefault/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void restoreDefaultService(@PathVariable("serviceId") String serviceId) {
        FtepService service = DefaultFtepServices.importDefaultService(serviceId);
        resetService(service);
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

    /**
     * Find all published services (READ permissions for PUBLIC) and republish them with new permissions (SERVICE_USER)
     */
    @PostMapping("/services/republishAll")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void republishAll() {
        for (FtepService service : serviceDataService.getAll()) {
            ObjectIdentity objectIdentity = new ObjectIdentityImpl(FtepService.class, service.getId());
            if (ftepSecurityService.isReadOnlyPublic(objectIdentity)) {
                LOG.debug("Republishing service " + service.getName());
                ftepSecurityService.publish(objectIdentity);
            }
        }
    }

    /**
     * Ensure service owners have SERVICE_OPERATOR permissions for all services they own
     */
    @PostMapping("/services/resetOwnerPermissions")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void resetOwnerPermissions() {
        for (FtepService service : serviceDataService.getAll()) {
            User owner = service.getOwner();
            LOG.debug("Setting SERVICE_OPERATOR permissions for service " + service.getName() + " for owner " + owner.getName());
            ftepSecurityService.setUserPermission(new ObjectIdentityImpl(FtepService.class, service.getId()), owner, FtepPermission.SERVICE_OPERATOR);
        }
    }

    /**
     * For groups that have some access to services, upgrade READ access to SERVICE_USER, and ADMIN and WRITE to SERVICE_OPERATOR
     */
    @PostMapping("/services/resetGroupPermissions")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN')")
    public void resetGroupPermissions() {
        for (Group group : groupDataService.getAll()) {
            Sid groupSid = new GrantedAuthoritySid(group);
            for (FtepService service : serviceDataService.getAll()) {
                ObjectIdentity objectIdentity = new ObjectIdentityImpl(FtepService.class, service.getId());
                if (ftepSecurityService.hasGroupPermission(objectIdentity, groupSid, FtepPermission.ADMIN)) {
                    LOG.debug("Upgrading group " + group.getId() + " permissions to service " + service.getName() + "from ADMIN to SERVICE_OPERATOR");
                    ftepSecurityService.setGroupPermission(objectIdentity, group, FtepPermission.SERVICE_OPERATOR);
                } else if (ftepSecurityService.hasGroupPermission(objectIdentity, groupSid, FtepPermission.WRITE)) {
                    LOG.debug("Upgrading group " + group.getName() + " permissions to service " + service.getName() + "from WRITE to SERVICE_OPERATOR");
                    ftepSecurityService.setGroupPermission(objectIdentity, group, FtepPermission.SERVICE_OPERATOR);
                } else if (ftepSecurityService.hasGroupPermission(objectIdentity, groupSid, FtepPermission.READ)) {
                    LOG.debug("Upgrading group " + group.getName() + " permissions to service " + service.getName() + "from READ to SERVICE_USER");
                    ftepSecurityService.setGroupPermission(objectIdentity, group, FtepPermission.SERVICE_USER);
                }
            }
        }
    }

    private void resetService(FtepService service) {
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
