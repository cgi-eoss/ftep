package com.cgi.eoss.ftep.orchestrator.data;

import com.cgi.eoss.ftep.model.FtepDatasource;
import com.cgi.eoss.ftep.model.rest.ApiEntity;
import com.cgi.eoss.ftep.model.rest.ResourceJob;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.Json;
import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class FtepJsonApi {

    // TODO Make API endpoints configurable
    private static final String API_LOGIN = "login";
    private static final String API_JOBS = "jobs";
    private static final String API_AUTHENTICATION = "datasources/getAuthentication";

    private final FtepApiUrl api;
    private final HttpTransport transport;
    private final ResourceConverter resourceJobConverter;
    private final ResourceConverter datsourceConverter;

    public FtepJsonApi(String baseUrl) {
        this(baseUrl, new NetHttpTransport());
    }

    public FtepJsonApi(String baseUrl, HttpTransport transport) {
        this.api = new FtepApiUrl(baseUrl);
        this.transport = transport;
        this.resourceJobConverter = new ResourceConverter(ResourceJob.class);
        this.datsourceConverter = new ResourceConverter(FtepDatasource.class);
    }

    public ApiEntity<ResourceJob> insert(ResourceJob job) throws IOException, IllegalAccessException {
        LOG.info("Saving job record {} via REST API", job.getJobId());

        HttpContent content = getHttpContentForResourceJob(job);
        HttpRequest request = transport.createRequestFactory()
                .buildPostRequest(api.jobs(), content)
                .setThrowExceptionOnExecuteError(true);

        JSONAPIDocument<ResourceJob> document = submitAndGetJsonApiResourceJob(request);
        return ApiEntity.<ResourceJob>builder()
                .resourceEndpoint(document.getLinks().getSelf().getHref())
                .resourceId(document.get().getId())
                .resource(document.get())
                .build();
    }

    public void update(ApiEntity<ResourceJob> apiJob) throws IOException, IllegalAccessException {
        HttpContent content = getHttpContentForResourceJob(apiJob.getResource());
        HttpRequest request = transport.createRequestFactory()
                .buildPatchRequest(new GenericUrl(apiJob.getResourceEndpoint()), content)
                .setThrowExceptionOnExecuteError(true);
        submitAndGetJsonApiResourceJob(request);
    }

    public FtepDatasource getDataSource(String host) throws IOException {
        HttpRequest request = transport.createRequestFactory().buildGetRequest(api.authentication(host));
        return submitAndGetJsonApiDatasource(request).get();
    }

    private JSONAPIDocument<ResourceJob> submitAndGetJsonApiResourceJob(HttpRequest request) throws IOException {
        LOG.debug("Submitting HTTP {} request to URL: {}", request.getRequestMethod(), request.getUrl());
        LOG.trace("HTTP request content: {}", request.getContent());

        HttpResponse response = request.execute();
        try {
            LOG.debug("Received HTTP response status: {} {}", response.getStatusCode(), response.getStatusMessage());
            return resourceJobConverter.readDocument(ByteStreams.toByteArray(response.getContent()), ResourceJob.class);
        } finally {
            response.disconnect();
        }
    }

    private JSONAPIDocument<FtepDatasource> submitAndGetJsonApiDatasource(HttpRequest request) throws IOException {
        LOG.debug("Submitting HTTP {} request to URL: {}", request.getRequestMethod(), request.getUrl());
        LOG.trace("HTTP request content: {}", request.getContent());

        HttpResponse response = request.execute();
        try {
            LOG.debug("Received HTTP response status: {} {}", response.getStatusCode(), response.getStatusMessage());
            return datsourceConverter.readDocument(ByteStreams.toByteArray(response.getContent()), FtepDatasource.class);
        } finally {
            response.disconnect();
        }
    }

    private HttpContent getHttpContentForResourceJob(ResourceJob job) throws JsonProcessingException, IllegalAccessException {
        byte[] bytes = resourceJobConverter.writeObject(job);
        return new ByteArrayContent(Json.MEDIA_TYPE, bytes);
    }

    private static final class FtepApiUrl {
        private final String baseUrl;

        public FtepApiUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public GenericUrl jobs() {
            GenericUrl url = new GenericUrl(baseUrl);
            url.appendRawPath(API_JOBS);
            return url;
        }

        public GenericUrl authentication(String host) {
            GenericUrl url = new GenericUrl(baseUrl);
            url.appendRawPath(API_AUTHENTICATION);
            url.appendRawPath(host);
            return url;
        }
    }

}
