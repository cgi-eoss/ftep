package com.cgi.eoss.ftep.search.scihub;

import com.cgi.eoss.ftep.search.api.RepoType;
import com.cgi.eoss.ftep.search.api.SearchParameters;
import com.cgi.eoss.ftep.search.api.SearchProvider;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.cgi.eoss.ftep.search.scihub.opensearch.OpenSearchResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j2
public class SciHubSearchProvider implements SearchProvider {

    private final Set<String> supportedMissions = ImmutableSet.of(
            "Sentinel-1",
            "Sentinel-2",
            "Sentinel-3"
    );

    private final HttpUrl baseUrl;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public SciHubSearchProvider(SciHubSearchProperties sciHubProperties, OkHttpClient httpClient) {
        this.baseUrl = sciHubProperties.getBaseUrl();
        this.client = httpClient.newBuilder()
                .addInterceptor(chain -> {
                    Request authenticatedRequest = chain.request().newBuilder()
                            .header("Authorization", Credentials.basic(sciHubProperties.getUsername(), sciHubProperties.getPassword()))
                            .build();
                    return chain.proceed(authenticatedRequest);
                })
                .addInterceptor(new HttpLoggingInterceptor(LOG::trace).setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .registerModules(new JavaTimeModule());
    }

    @Override
    public SearchResults search(SearchParameters parameters) throws IOException {
        HttpUrl.Builder httpUrl = baseUrl.newBuilder()
                .addQueryParameter("start", "0")
                .addQueryParameter("rows", "20")
                .addQueryParameter("q", "*")
                .addQueryParameter("format", "JSON");

        Request request = new Request.Builder().url(httpUrl.build()).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected HTTP response code from SciHub: " + response);
            }
            OpenSearchResult sciHubResult = objectMapper.readValue(response.body().string(), OpenSearchResult.class);
            LOG.info(sciHubResult);
        }

        // TODO Implement
        return null;
    }

    @Override
    public List<SearchResults.Link> getPagingLinks() {
        return ImmutableList.of();
    }

    @Override
    public Map<String, String> getPagingParameters(SearchParameters parameters) {
        return ImmutableMap.of();
    }

    @Override
    public Map<String, String> getQueryParameters(SearchParameters parameters) {
        return ImmutableMap.of();
    }

    @Override
    public boolean supports(RepoType repoType, SearchParameters parameters) {
        // TODO Make configurable
        return repoType == RepoType.SATELLITE &&
                supportedMissions.containsAll(parameters.getOtherParameters().get("mission"));
    }

}
