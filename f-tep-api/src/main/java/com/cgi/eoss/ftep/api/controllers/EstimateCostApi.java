package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.costing.CostingService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.internal.CostQuotation;
import com.cgi.eoss.ftep.search.api.SearchFacade;
import com.cgi.eoss.ftep.search.api.SearchParameters;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

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
    private final SearchFacade searchFacade;

    @Autowired
    public EstimateCostApi(CostingService costingService, FtepSecurityService ftepSecurityService, SearchFacade searchFacade) {
        this.costingService = costingService;
        this.ftepSecurityService = ftepSecurityService;
        this.searchFacade = searchFacade;
    }

    @GetMapping("/jobConfig/{jobConfigId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'read')")
    public ResponseEntity estimateJobConfigCost(@ModelAttribute("jobConfigId") JobConfig jobConfig) {
        int walletBalance = ftepSecurityService.getCurrentUser().getWallet().getBalance();

        try {
            int cost = costingService.estimateJobCost(jobConfig);
            return ResponseEntity
                    .status(cost > walletBalance ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK)
                    .body(CostEstimationResponse.builder().estimatedCost(cost).currentWalletBalance(walletBalance).build());
        } catch (com.cgi.eoss.ftep.batch.service.JobExpansionException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error evaluating Job parameters: " + e.getMessage());
        }
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

    @PostMapping("/systematic")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or (#jobConfigTemplate.id == null) or hasPermission(#jobConfigTemplate, 'read')")
    public ResponseEntity estimateSystematicCost(HttpServletRequest request, @RequestBody JobConfig jobConfigTemplate) throws InterruptedException, JsonParseException, JsonMappingException, JsonProcessingException, IOException {
        int walletBalance = ftepSecurityService.getCurrentUser().getWallet().getBalance();
        CostQuotation singleRunCostEstimate = costingService.estimateSystematicJobCost(jobConfigTemplate);

        Map<String, String[]> requestParameters = request.getParameterMap();
        ListMultimap<String, String> queryParameters = ArrayListMultimap.create();
        for (Map.Entry<String, String[]> entry : requestParameters.entrySet()) {
            queryParameters.putAll(entry.getKey(), Arrays.asList(entry.getValue()));
        }

        //Run the query to assess the rough number of results for last month
        LocalDateTime start = LocalDateTime.now().minusDays(31);
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        String productDateStart = ZonedDateTime.of(start, ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String productDateEnd = ZonedDateTime.of(end, ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        queryParameters.replaceValues("productDateStart", Collections.singletonList(productDateStart));
        queryParameters.replaceValues("productDateEnd", Collections.singletonList(productDateEnd));

        SearchParameters searchParameters = new SearchParameters();
        searchParameters.setParameters(queryParameters);
        searchParameters.setRequestUrl(HttpUrl.parse("http://ftep-estimate"));

        SearchResults results = searchFacade.search(searchParameters);

        //Overwrite query params using last month
        int monthlyCost = (int) results.getPage().getTotalElements() * singleRunCostEstimate.getCost();

        return ResponseEntity
                .status(monthlyCost > walletBalance ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK)
                .body(CostEstimationResponse.builder()
                        .estimatedCost(monthlyCost)
                        .recurrence(CostQuotation.Recurrence.MONTHLY)
                        .currentWalletBalance(walletBalance)
                        .build());
    }

    @Data
    @Builder
    private static class CostEstimationResponse {
        int estimatedCost;
        int currentWalletBalance;
        CostQuotation.Recurrence recurrence;
    }

}
