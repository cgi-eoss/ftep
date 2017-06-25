package com.cgi.eoss.ftep.search.ftep;

import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.cgi.eoss.ftep.search.api.RepoType;
import com.cgi.eoss.ftep.search.api.SearchParameters;
import com.cgi.eoss.ftep.search.api.SearchProvider;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.cgi.eoss.ftep.search.ftep.opensearch.RestoSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class FtepRestoSearchProvider implements SearchProvider {

    private final HttpUrl baseUrl;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final RestoService restoService;
    private final FtepFileDataService ftepFileDataService;

    public FtepRestoSearchProvider(FtepSearchProperties ftepSearchProperties, OkHttpClient httpClient, ObjectMapper objectMapper, RestoService restoService, FtepFileDataService ftepFileDataService) {
        this.baseUrl = ftepSearchProperties.getBaseUrl();
        this.client = httpClient.newBuilder()
                .addInterceptor(chain -> {
                    Request authenticatedRequest = chain.request().newBuilder()
                            .header("Authorization", Credentials.basic(ftepSearchProperties.getUsername(), ftepSearchProperties.getPassword()))
                            .build();
                    return chain.proceed(authenticatedRequest);
                })
                .addInterceptor(new HttpLoggingInterceptor(LOG::trace).setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        this.objectMapper = objectMapper;
        this.restoService = restoService;
        this.ftepFileDataService = ftepFileDataService;
    }

    @Override
    public SearchResults search(SearchParameters parameters) throws IOException {
        String collectionName = getCollection(parameters.getRepo());

        ListMultimap<String, String> otherParameters = parameters.getOtherParameters();

        HttpUrl.Builder httpUrl = baseUrl.newBuilder()
                .addPathSegments("api/collections").addPathSegment(collectionName).addPathSegment("search.json");

        getPagingParameters(parameters).forEach(httpUrl::addQueryParameter);
        getQueryParameters(parameters).forEach(httpUrl::addQueryParameter);

        Request request = new Request.Builder().url(httpUrl.build()).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected HTTP response code from Resto: " + response);
            }
            RestoSearchResult restoResult = objectMapper.readValue(response.body().string(), RestoSearchResult.class);

            return SearchResults.builder()
                    .parameters(parameters)
                    .paging(SearchResults.Paging.builder()
                            .totalResults(restoResult.getProperties().getTotalResultsCount())
                            .exactCount(restoResult.getProperties().isExactCount())
                            .itemsPerPage(restoResult.getProperties().getItemsPerPage())
                            .startIndex(restoResult.getProperties().getStartIndex())
                            .build())
                    .features(restoResult.getFeatures())
                    .links(getPagingLinks())
                    .build();
        }
    }

    @Override
    public List<SearchResults.Link> getPagingLinks() {
        // TODO Calculate paging links
        return ImmutableList.of();
    }

    @Override
    public Map<String, String> getPagingParameters(SearchParameters parameters) {
        Map<String, String> pagingParameters = new HashMap<>();
        pagingParameters.put("maxRecords", Integer.toString(parameters.getResultsPerPage()));
        pagingParameters.put("page", Integer.toString(parameters.getPage()));
        return pagingParameters;
    }

    @Override
    public Map<String, String> getQueryParameters(SearchParameters parameters) {
        Map<String, String> queryParameters = new HashMap<>();

        ListMultimap<String, String> otherParameters = parameters.getOtherParameters();

        if (!otherParameters.get("keyword").isEmpty()) {
            queryParameters.put("productIdentifier", "%" + otherParameters.get("keyword").get(0) + "%");
        }

        if (!otherParameters.get("geometry").isEmpty()) {
            queryParameters.put("geometry", otherParameters.get("geometry").get(0));
        }

        if (!otherParameters.get("owner").isEmpty()) {
            queryParameters.put("owner", otherParameters.get("owner").get(0));
        }

        return queryParameters;
    }

    @Override
    public boolean supports(RepoType repoType, SearchParameters parameters) {
        return repoType == RepoType.FTEP_PRODUCTS || repoType == RepoType.REF_DATA;
    }

    private String getCollection(RepoType repoType) {
        switch (repoType) {
            case REF_DATA:
                return restoService.getReferenceDataCollection();
            case FTEP_PRODUCTS:
                return restoService.getOutputProductsCollection();
            default:
                throw new IllegalArgumentException("Could not identify Resto collection for repo type: " + repoType);
        }
    }

}
