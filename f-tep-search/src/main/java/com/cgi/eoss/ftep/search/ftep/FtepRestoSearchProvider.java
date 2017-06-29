package com.cgi.eoss.ftep.search.ftep;

import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.search.api.RepoType;
import com.cgi.eoss.ftep.search.api.SearchParameters;
import com.cgi.eoss.ftep.search.api.SearchProvider;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.cgi.eoss.ftep.search.ftep.opensearch.RestoSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
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
import java.util.Map;

@Log4j2
public class FtepRestoSearchProvider implements SearchProvider {

    private final HttpUrl baseUrl;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final RestoService restoService;

    public FtepRestoSearchProvider(FtepSearchProperties ftepSearchProperties, OkHttpClient httpClient, ObjectMapper objectMapper, RestoService restoService) {
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

            SearchResults.Page page = getPageInfo(restoResult);
            return SearchResults.builder()
                    .parameters(parameters)
                    .page(page)
                    .features(restoResult.getFeatures())
                    .links(getLinks(parameters.getRequestUrl(), page, restoResult))
                    .build();
        }
    }

    private SearchResults.Page getPageInfo(RestoSearchResult restoResult) {
        long totalResultsCount = restoResult.getProperties().getTotalResultsCount();
        long itemsPerPage = restoResult.getProperties().getItemsPerPage();
        long startIndex = restoResult.getProperties().getStartIndex();
        long pageNumber = startIndex / itemsPerPage;
        long totalPages = totalResultsCount - startIndex < itemsPerPage ? (totalResultsCount / itemsPerPage) : (totalResultsCount / itemsPerPage) + 1;

        return SearchResults.Page.builder()
                .totalElements(totalResultsCount)
                .size(itemsPerPage)
                .number(pageNumber)
                .totalPages(totalPages)
                .build();
    }

    @Override
    public Map<String, String> getPagingParameters(SearchParameters parameters) {
        Map<String, String> pagingParameters = new HashMap<>();
        pagingParameters.put("maxRecords", Integer.toString(parameters.getResultsPerPage()));
        pagingParameters.put("page", Integer.toString(parameters.getPage() + 1));
        return pagingParameters;
    }

    @Override
    public Map<String, String> getQueryParameters(SearchParameters parameters) {
        Map<String, String> queryParameters = new HashMap<>();

        ListMultimap<String, String> otherParameters = parameters.getOtherParameters();

        if (!otherParameters.get("keyword").isEmpty() && !Strings.isNullOrEmpty(otherParameters.get("keyword").get(0))) {
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

    private Map<String, SearchResults.Link> getLinks(HttpUrl requestUrl, SearchResults.Page page, RestoSearchResult restoResult) {
        Map<String, SearchResults.Link> links = new HashMap<>();

        // Paging links:
        links.put("first", getPageLink("first", requestUrl, 0));
        if (page.getNumber() != 1) {
            links.put("prev", getPageLink("prev", requestUrl, page.getNumber() - 1));
        }
        if (page.getNumber() != page.getTotalPages() - 1) {
            links.put("next", getPageLink("next", requestUrl, page.getNumber() + 1));
        }
        links.put("last", getPageLink("last", requestUrl, page.getTotalPages() - 1));

        return links;
    }

    private SearchResults.Link getPageLink(String rel, HttpUrl requestUrl, long page) {
        return SearchResults.Link.builder().rel(rel).href(requestUrl.newBuilder().removeAllQueryParameters("page").addQueryParameter("page", String.valueOf(page)).build().toString()).build();
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
