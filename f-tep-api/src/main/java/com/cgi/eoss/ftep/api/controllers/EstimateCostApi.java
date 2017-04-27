package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.costing.CostingService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.JobConfig;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Functionality for users to retrieve cost estimations for F-TEP activities.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/estimateCost")
@Log4j2
public class EstimateCostApi {

    private final CostingService costingService;
    private final FtepSecurityService ftepSecurityService;

    @Autowired
    public EstimateCostApi(CostingService costingService, FtepSecurityService ftepSecurityService) {
        this.costingService = costingService;
        this.ftepSecurityService = ftepSecurityService;
    }

    @GetMapping("/jobConfig/{jobConfigId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'read')")
    public ResponseEntity estimateJobConfigCost(@ModelAttribute("jobConfigId") JobConfig jobConfig) {
        int walletBalance = ftepSecurityService.getCurrentUser().getWallet().getBalance();
        int cost = costingService.estimateJobCost(jobConfig);

        return ResponseEntity
                .status(cost > walletBalance ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK)
                .body(CostEstimationResponse.builder().estimatedCost(cost).currentWalletBalance(walletBalance).build());
    }

    @GetMapping("/download/{ftepFileId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#ftepFile, 'read')")
    public ResponseEntity estimateDownloadCost(@ModelAttribute("ftepFileId") FtepFile ftepFile) {
        int walletBalance = ftepSecurityService.getCurrentUser().getWallet().getBalance();
        int cost = costingService.estimateDownloadCost(ftepFile);

        return ResponseEntity
                .status(cost > walletBalance ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK)
                .body(CostEstimationResponse.builder().estimatedCost(cost).currentWalletBalance(walletBalance).build());
    }

    @Data
    @Builder
    private static class CostEstimationResponse {
        int estimatedCost;
        int currentWalletBalance;
    }

}
