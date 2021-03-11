package com.cgi.eoss.ftep.api.controllers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GraylogClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${ftep.api.logs.graylogApiUrl:http://ftep-monitor:8087/log/api}")
    private String graylogApiUrl;

    public GraylogClient(@Value("${ftep.api.logs.username:admin}") String username,
                         @Value("${ftep.api.logs.password:graylogpass}") String password) {
        this.httpClient = new OkHttpClient.Builder().addInterceptor((Interceptor.Chain chain) -> {
            Request request = chain.request();
            Request authenticatedRequest = request.newBuilder()
                    .header("Authorization", Credentials.basic(username, password))
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .build();
            return chain.proceed(authenticatedRequest);
        }).addInterceptor(new HttpLoggingInterceptor(LOG::trace).setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<SimpleMessage> loadMessages(Map<String, String> parameters) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(graylogApiUrl).newBuilder().addPathSegments("search/universal/relative");
        parameters.forEach(urlBuilder::addEncodedQueryParameter);
        HttpUrl searchUrl = urlBuilder.build();

        List<SimpleMessage> messages = new ArrayList<>();
        LOG.debug("Retrieving logs from url: {}", searchUrl);
        loadGraylogMessages(messages, searchUrl);
        return messages;
    }

    public void loadGraylogMessages(List<SimpleMessage> messages, HttpUrl graylogApiUrl) throws IOException {
        Request request = new Request.Builder()
                .get()
                .url(graylogApiUrl)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                GraylogApiResponse graylogApiResponse = objectMapper.readValue(response.body().string(), GraylogApiResponse.class);
                graylogApiResponse.getMessages().stream()
                        .map(GraylogMessage::getMessage)
                        .forEach(messages::add);

                if (messages.size() < graylogApiResponse.getTotalResults()) {
                    loadGraylogMessages(messages, graylogApiUrl.newBuilder().setQueryParameter("offset", String.valueOf(messages.size())).build());
                }
            } else {
                if (response.code() != 503) {
                    LOG.error("Failed to retrieve logs: {} -- {}", response.code(), response.message());
                }
                LOG.debug("Graylog response: {}", response.body());
            }
        }
    }

    /**
     * This function executes a custom search on the Graylog search REST API and
     * return the results mapped into Map<String, Object>.
     * @param urlPathSegments
     * @param queryParameters
     */
    public Map<String, Object> loadGraylogCustomSearch(String urlPathSegments, Map<String, String> queryParameters) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(graylogApiUrl).newBuilder().addPathSegments(urlPathSegments);
        queryParameters.forEach(urlBuilder::addQueryParameter);
        Request request = new Request.Builder()
                .get()
                .header("Accept", "application/json")
                .url(urlBuilder.build())
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                // Map up the JSON into key-value pairs
                String respBodyString = response.body().string();
                ObjectMapper mapper = new ObjectMapper();
                LOG.debug("Body:\n" + respBodyString);
                return mapper.readValue(respBodyString, new TypeReference<Map<String, Object>>() {
                });
            } else {
                if (response.code() != 503) {
                    LOG.error("Failed to retrieve custom search results: {} -- {}", response.code(), response.message());
                }
                LOG.debug("Graylog response for URL: {}\n{}", request.url(), response);
            }
        } catch (IOException ioe) {
            LOG.warn("Unsuccessful mapping on JSON data; reason:\n" + ioe.getMessage());
        }
        return Collections.EMPTY_MAP;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GraylogApiResponse {
        private List<GraylogMessage> messages;
        @JsonProperty("total_results")
        private Long totalResults;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GraylogMessage {
        private SimpleMessage message;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class SimpleMessage {
        private String timestamp;
        private String message;
    }

}
