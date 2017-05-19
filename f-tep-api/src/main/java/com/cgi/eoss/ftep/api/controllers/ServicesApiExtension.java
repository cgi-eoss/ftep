package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.persistence.service.ServiceDataService;
import com.cgi.eoss.ftep.services.DefaultFtepServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

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
                .map(s -> serviceDataService.getByName(s.getName()))
                .collect(Collectors.toList()));
    }

}
