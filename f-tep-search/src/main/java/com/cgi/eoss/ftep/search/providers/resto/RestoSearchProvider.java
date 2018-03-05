package com.cgi.eoss.ftep.search.providers.resto;

import com.cgi.eoss.ftep.search.api.SearchParameters;
import com.cgi.eoss.ftep.search.api.SearchProvider;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.geojson.Feature;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Common parent class for search providers interacting with Resto catalogues.</p> <p>Provides adapters for
 * pagination and common search parameters.</p>
 */
@Log4j2
public abstract class RestoSearchProvider implements SearchProvider {

    private final HttpUrl baseUrl;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public RestoSearchProvider(HttpUrl baseUrl, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.client = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public SearchResults search(SearchParameters parameters) throws IOException {
        String collectionName = getCollection(parameters);

        HttpUrl.Builder httpUrl = baseUrl.newBuilder().addPathSegments("api/collections").addPathSegment(collectionName).addPathSegment("search.json");

        getPagingParameters(parameters).forEach(httpUrl::addQueryParameter);
        getQueryParameters(parameters).forEach(httpUrl::addQueryParameter);

        Request request = new Request.Builder().url(httpUrl.build()).get().build();

        LOG.debug("Performing HTTP request for Resto search: {}", httpUrl);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for Resto search: {}", response.toString());
                throw new IOException("Unexpected HTTP response code from Resto: " + response);
            }
            LOG.info("Received successful response for Resto search: {}", request.url());
            RestoResult restoResult = objectMapper.readValue(response.body().string(), RestoResult.class);

            SearchResults.Page page = getPageInfo(parameters, restoResult);
            return postProcess(SearchResults.builder()
                    .parameters(parameters)
                    .page(page)
                    .features(restoResult.getFeatures())
                    .links(getLinks(parameters.getRequestUrl(), page, restoResult))
                    .build());
        } catch (Exception e) {
            LOG.error("Could not perform HTTP request for Resto search", e);
            throw e;
        }
    }

    @Override
    public Map<String, String> getPagingParameters(SearchParameters parameters) {
        Map<String, String> pagingParameters = new HashMap<>();
        pagingParameters.put("maxRecords", Integer.toString(parameters.getResultsPerPage()));
        pagingParameters.put("page", Integer.toString(parameters.getPage() + 1));
        return pagingParameters;
    }

    protected Map<String, SearchResults.Link> getPagingLinks(SearchResults.Page page, HttpUrl requestUrl) {
        Map<String, SearchResults.Link> links = new HashMap<>();
        links.put("first", getPageLink("first", requestUrl, 0));
        if (page.getNumber() > 0) {
            links.put("prev", getPageLink("prev", requestUrl, page.getNumber() - 1));
        }
        if (page.getNumber() < page.getTotalPages() - 1) {
            links.put("next", getPageLink("next", requestUrl, page.getNumber() + 1));
        }
        links.put("last", getPageLink("last", requestUrl, page.getTotalPages() - 1));
        return links;
    }

    protected static Long getRestoFilesize(Feature f) {
        return ((Number)
                ((Map<String, Object>)
                    ((Map<String, Object>)
                        f.getProperties().get("services"))
                            .get("download")
                ).get("size")
            ).longValue();
    }

    protected abstract Map<String, SearchResults.Link> getLinks(HttpUrl requestUrl, SearchResults.Page page, RestoResult restoResult);

    protected abstract String getCollection(SearchParameters parameters);

    protected abstract SearchResults postProcess(SearchResults results);

    private SearchResults.Page getPageInfo(SearchParameters parameters, RestoResult restoResult) {
        long queryResultsPerPage = parameters.getResultsPerPage();
        long queryPage = parameters.getPage();

        long totalResultsCount = restoResult.getProperties().getTotalResultsCount();
        long countOnPage = restoResult.getProperties().getItemsPerPage();
        long totalPages = (totalResultsCount / queryResultsPerPage) + 1;

        return SearchResults.Page.builder()
                .totalElements(totalResultsCount)
                .size(countOnPage)
                .number(queryPage)
                .totalPages(totalPages)
                .build();
    }

    private SearchResults.Link getPageLink(String rel, HttpUrl requestUrl, long page) {
        return SearchResults.Link.builder().rel(rel).href(requestUrl.newBuilder().removeAllQueryParameters("page").addQueryParameter("page", String.valueOf(page)).build().toString()).build();
    }

}
