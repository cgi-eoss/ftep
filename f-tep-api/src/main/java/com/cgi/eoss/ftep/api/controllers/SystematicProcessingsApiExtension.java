package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.SystematicProcessing;
import com.cgi.eoss.ftep.persistence.service.SystematicProcessingDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@BasePathAwareController
@RequestMapping("/systematicProcessings")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class SystematicProcessingsApiExtension {

    private final SystematicProcessingDataService systematicProcessingDataService;

    @PostMapping("/{systematicProcessingId}/terminate")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#systematicProcessing, 'write')")
    public ResponseEntity terminate(@ModelAttribute("systematicProcessingId") SystematicProcessing systematicProcessing) {
        LOG.debug("Terminating systematic processing: {}", systematicProcessing.getId());
        systematicProcessing.setStatus(SystematicProcessing.Status.COMPLETED);
        systematicProcessingDataService.save(systematicProcessing);
        return ResponseEntity.noContent().build();
    }

}