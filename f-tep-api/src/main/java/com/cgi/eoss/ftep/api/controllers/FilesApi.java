package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * <p>A {@link RestController} for interacting with {@link com.cgi.eoss.ftep.model.FtepFile}s.</p>
 */
@RestController
@RequestMapping("/files")
@Slf4j
public class FilesApi {

    private final FtepSecurityUtil ftepSecurityUtil;
    private final CatalogueService catalogueService;

    @Autowired
    public FilesApi(FtepSecurityUtil ftepSecurityUtil, CatalogueService catalogueService) {
        this.ftepSecurityUtil = ftepSecurityUtil;
        this.catalogueService = catalogueService;
    }

    @PostMapping("/refData/new")
    public FtepFile saveRefData(
            @RequestParam("geometry") String geometry,
            @RequestParam("file") MultipartFile file) throws IOException {
        User user = ftepSecurityUtil.getCurrentUser();
        try {
            return catalogueService.createReferenceData(user, file.getOriginalFilename(), geometry, file);
        } catch (IOException e) {
            LOG.error("Could not store reference data file {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    @GetMapping("/{fileId}/dl")
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#file, 'read')")
    public ResponseEntity<Resource> downloadFile(@ModelAttribute("fileId") FtepFile file) {
        Resource fileResource = catalogueService.getAsResource(file);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileResource.getFilename() + "\"")
                .body(fileResource);
    }

}
