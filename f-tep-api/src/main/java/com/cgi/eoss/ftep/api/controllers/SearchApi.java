package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.cgi.eoss.ftep.search.api.SearchFacade;
import com.cgi.eoss.ftep.search.api.SearchParameters;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.ByteStreams;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.util.Map;

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
    private final FtepFilesApiExtension ftepFilesApiExtension;
    private final FtepFileDataService ftepFileDataService;

    @Autowired
    public SearchApi(SearchFacade searchFacade, ObjectMapper objectMapper, FtepFilesApiExtension ftepFilesApiExtension, FtepFileDataService ftepFileDataService) {
        // Handle single-/multi-value parameters
        this.objectMapper = objectMapper.copy()
                .configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, true)
                .configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        this.searchFacade = searchFacade;
        this.ftepFilesApiExtension = ftepFilesApiExtension;
        this.ftepFileDataService = ftepFileDataService;
    }

    @GetMapping("/parameters")
    public Map<String, Object> getAvailableParameters(HttpServletRequest request) throws IOException, URISyntaxException {
        return searchFacade.getParametersSchema();
    }

    @GetMapping
    public SearchResults search(HttpServletRequest request) throws IOException {
        // Do a two-way serialize/deserialize to convert the clumsy Map<String, String[]> to a nice SearchParameters object
        SearchParameters parameters = objectMapper.readValue(objectMapper.writeValueAsString(request.getParameterMap()), SearchParameters.class);
        parameters.setRequestUrl(HttpUrl.parse(ServletUriComponentsBuilder.fromCurrentServletMapping().toUriString()).newBuilder()
                .encodedPath(request.getServletPath())
                .encodedQuery(request.getQueryString()).build());
        return searchFacade.search(parameters);
    }

    @GetMapping("/ql/{productSource}/{productIdentifier}")
    public void quicklook(@PathVariable("productSource") String productSource, @PathVariable("productIdentifier") String productIdentifier, HttpServletResponse response) throws IOException {
        try {
            Resource fileResource = searchFacade.getQuicklookResource(productSource, productIdentifier);

            response.setContentType("image/unknown");
            response.setContentLengthLong(fileResource.contentLength());
            response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileResource.getFilename() + "\"");
            ByteStreams.copy(fileResource.getInputStream(), response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            LOG.error("Could not load quicklook for {}", productIdentifier, e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            new OutputStreamWriter(response.getOutputStream()).write("Could not load quicklook");
            response.flushBuffer();
        }
    }

    @GetMapping("/dl/{productSource}/{productIdentifier}")
    public void downloadFile(@PathVariable("productSource") String productSource, @PathVariable("productIdentifier") String productIdentifier, HttpServletResponse response) throws IOException {
        switch (productSource) {
            case "ftep":
                ftepFilesApiExtension.downloadFile(ftepFileDataService.getById(Long.valueOf(productIdentifier)), response);
                break;
            default:
                LOG.error("Could not locate download for {}://{}", productSource, productIdentifier);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                new OutputStreamWriter(response.getOutputStream()).write("Could not locate download");
                response.flushBuffer();
        }
    }

}
