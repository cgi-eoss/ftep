package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.internal.ReferenceDataMetadata;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
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
            @RequestParam("file") MultipartFile file) throws Exception {
        try {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(file.getOriginalFilename()), "Uploaded filename may not be null {}", file);

            ReferenceDataMetadata metadata = ReferenceDataMetadata.builder()
                    .owner(ftepSecurityUtil.getCurrentUser())
                    .filename(file.getOriginalFilename())
                    .geometry(geometry)
                    .properties(ImmutableMap.of()) // TODO Collect user-driven metadata properties
                    .build();

            return catalogueService.ingestReferenceData(metadata, file);
        } catch (Exception e) {
            LOG.error("Could not ingest reference data file {}", file.getOriginalFilename(), e);
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
