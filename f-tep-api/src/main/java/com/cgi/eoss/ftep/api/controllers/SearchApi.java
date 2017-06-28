package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.cgi.eoss.ftep.search.api.SearchFacade;
import com.cgi.eoss.ftep.search.api.SearchParameters;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * <p>Functionality for accessing the F-TEP unifying search facade.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/search")
@Log4j2
public class SearchApi {

    private final SearchFacade searchFacade;
    private final ObjectMapper objectMapper;
    private final FtepFileDataService ftepFileDataService;
    private final FtepSecurityService securityService;

    @Autowired
    public SearchApi(SearchFacade searchFacade, ObjectMapper objectMapper, FtepFileDataService ftepFileDataService, FtepSecurityService securityService) {
        // Handle single-/multi-value parameters
        this.objectMapper = objectMapper.copy()
                .configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, true)
                .configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        this.searchFacade = searchFacade;
        this.ftepFileDataService = ftepFileDataService;
        this.securityService = securityService;
    }

    @GetMapping
    public SearchResults searchResults(WebRequest webRequest) throws IOException {
        // Do a two-way serialize/deserialize to convert the clumsy Map<String, String[]> to a nice SearchParameters object
        SearchParameters parameters = objectMapper.readValue(objectMapper.writeValueAsString(webRequest.getParameterMap()), SearchParameters.class);
        SearchResults results = searchFacade.search(parameters);

        // Add visibility info, if the result can be matched to an FtepFile
        results.getFeatures().forEach(f -> {
            // Default to usable
            boolean ftepUsable = true;
            URI ftepUri = null;
            try {
                FtepFile ftepFile = ftepFileDataService.getByRestoId(UUID.fromString(f.getId()));
                if (ftepFile != null) {
                    ftepUsable = securityService.isReadableByCurrentUser(FtepFile.class, ftepFile.getId());
                    ftepUri = ftepFile.getUri();
                } else {
                    LOG.debug("No FtepFile found for search result with ID: {}", f.getId());
                }
            } catch (Exception e) {
                LOG.debug("Could not check visibility of search result with ID: {}", f.getId(), e);
            }
            f.getProperties().put("ftepUsable", ftepUsable);
            f.getProperties().put("ftepUri", ftepUri);
        });
        return results;
    }

}
