package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.PublishingRequest;
import com.cgi.eoss.ftep.persistence.service.PublishingRequestDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@BasePathAwareController
@RequestMapping("/publishingRequests")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class PublishingRequestsApiExtension {

    private final PublishingRequestDataService dataService;
    private final RepositoryEntityLinks entityLinks;
    private final FtepSecurityService ftepSecurityService;

    @PostMapping("/requestPublishService/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public ResponseEntity requestPublishService(@ModelAttribute("serviceId") FtepService service) {
        PublishingRequest newRequest = PublishingRequest.builder()
                .owner(ftepSecurityService.getCurrentUser())
                .type(PublishingRequest.Type.SERVICE)
                .associatedId(service.getId())
                .status(PublishingRequest.Status.REQUESTED)
                .build();

        PublishingRequest persistent = dataService.findOneByExample(newRequest);

        if (persistent != null) {
            // Re-request if necessary
            persistent.setStatus(PublishingRequest.Status.REQUESTED);
            return ResponseEntity.noContent().location(URI.create(entityLinks.linkToSingleResource(persistent).expand().getHref())).build();
        } else {
            persistent = dataService.save(newRequest);
            return ResponseEntity.created(URI.create(entityLinks.linkToSingleResource(persistent).expand().getHref())).build();
        }
    }

}
