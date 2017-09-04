package com.cgi.eoss.ftep.search.providers.ceda;

import com.cgi.eoss.ftep.catalogue.external.ExternalProductDataService;
import com.cgi.eoss.ftep.search.api.SearchParameters;
import com.cgi.eoss.ftep.search.api.SearchProvider;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.geojson.Feature;
import org.geojson.Polygon;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class CedaSearchProvider implements SearchProvider {

    private static final BiMap<String, String> SUPPORTED_MISSIONS = ImmutableBiMap.of(
            "landsat", "Landsat",
            "sentinel1", "Sentinel-1",
            "sentinel2", "Sentinel-2",
            "sentinel3", "Sentinel-3"
    );
    private static final Set<String> INTERNAL_FTEP_PARAMS = ImmutableSet.of(
            "catalogue"
    );
    private static final Map<String, String> PARAMETER_NAME_MAPPING = ImmutableMap.<String, String>builder()
            .put("identifier", "name")
            .put("aoi", "geometry")
            .put("productDateStart", "startDate")
            .put("productDateEnd", "endDate")
            .put("maxCloudCover", "maxCloudCoverPercentage")
            .put("s1ProductType", "productType")
            .build();
    private static final Map<String, Function<String, String>> PARAMETER_VALUE_MAPPING = ImmutableMap.<String, Function<String, String>>builder()
            .put("productDateStart", v -> DateTimeFormatter.ISO_DATE_TIME.format(LocalDate.from(DateTimeFormatter.ISO_DATE_TIME.parse(v)).atStartOfDay(ZoneOffset.UTC)))
            .put("productDateEnd", v -> DateTimeFormatter.ISO_DATE_TIME.format(LocalDate.from(DateTimeFormatter.ISO_DATE_TIME.parse(v)).plusDays(1).atStartOfDay(ZoneOffset.UTC)))
            .put("mission", CedaSearchProvider::translateMissionParameter)
            .put("platform", CedaSearchProvider::translatePlatformParameter)
            .put("orbitDirection", String::toLowerCase)
            .build();

    private final int priority;
    private final HttpUrl baseUrl;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final ExternalProductDataService externalProductService;
    private final boolean usableProductsOnly;
    private final CedaQuicklooksCache quicklooksCache;

    public CedaSearchProvider(int priority, CedaSearchProperties searchProperties, OkHttpClient httpClient, ObjectMapper objectMapper, ExternalProductDataService externalProductService, CedaQuicklooksCache quicklooksCache) {
        this.priority = priority;
        this.baseUrl = searchProperties.getBaseUrl();
        this.client = httpClient.newBuilder().addInterceptor(new HttpLoggingInterceptor(LOG::trace).setLevel(HttpLoggingInterceptor.Level.BODY)).build();
        this.usableProductsOnly = searchProperties.isUsableProductsOnly();
        this.objectMapper = objectMapper;
        this.externalProductService = externalProductService;
        this.quicklooksCache = quicklooksCache;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public SearchResults search(SearchParameters parameters) throws IOException {
        HttpUrl.Builder httpUrl = baseUrl.newBuilder();

        getPagingParameters(parameters).forEach(httpUrl::addQueryParameter);
        getQueryParameters(parameters).forEach(httpUrl::addQueryParameter);

        Request request = new Request.Builder().url(httpUrl.build()).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.error("Received unsuccessful HTTP response for CEDA search: {}", response.toString());
                throw new IOException("Unexpected HTTP response from CEDA: " + response.message());
            }
            LOG.info("Received successful response for CEDA search: {}", request.url());
            CedaResult cedaResult = objectMapper.readValue(response.body().string(), CedaResult.class);

            SearchResults.Page page = getPageInfo(parameters, cedaResult);
            return SearchResults.builder()
                    .parameters(parameters)
                    .page(page)
                    .features(transformCedaResult(parameters, cedaResult))
                    .links(getLinks(parameters.getRequestUrl(), page, response.request().url(), cedaResult))
                    .build();
        }
    }

    @Override
    public Map<String, String> getPagingParameters(SearchParameters parameters) {
        if ((parameters.getResultsPerPage() * (parameters.getPage() + 1)) > 10000) {
            throw new UnsupportedOperationException("CEDA search does not permit paging past the first 10,000 results; please add additional query constraints to narrow the search");
        }

        Map<String, String> pagingParameters = new HashMap<>();
        pagingParameters.put("maximumRecords", Integer.toString(parameters.getResultsPerPage()));
        pagingParameters.put("startPage", Integer.toString(parameters.getPage() + 1));
        return pagingParameters;
    }

    @Override
    public Map<String, String> getQueryParameters(SearchParameters parameters) {
        Map<String, String> queryParameters = new HashMap<>();

        parameters.getParameters().asMap().entrySet().stream()
                .filter(p -> !INTERNAL_FTEP_PARAMS.contains(p.getKey()) && !p.getValue().isEmpty() && !Strings.isNullOrEmpty(Iterables.getFirst(p.getValue(), null)))
                .forEach(p -> addTransformedParameter(queryParameters, p));

        if (usableProductsOnly) {
            queryParameters.put("dataOnline", "true");
        }

        return queryParameters;
    }

    private void addTransformedParameter(Map<String, String> queryParameters, Map.Entry<String, Collection<String>> parameter) {
        String parameterName = parameter.getKey();
        String parameterValue = Iterables.getFirst(parameter.getValue(), null);

        queryParameters.put(
                Optional.ofNullable(PARAMETER_NAME_MAPPING.get(parameterName)).orElse(parameterName),
                Optional.ofNullable(PARAMETER_VALUE_MAPPING.get(parameterName)).map(f -> f.apply(parameterValue)).orElse(parameterValue)
        );
    }

    @Override
    public boolean supports(SearchParameters parameters) {
        String catalogue = parameters.getValue("catalogue", "UNKNOWN");
        return catalogue.equals("SATELLITE") &&
                SUPPORTED_MISSIONS.keySet().contains(parameters.getValue("mission", "UNKNOWN"));
    }

    @Override
    public boolean supportsQuicklook(String productSource, String productIdentifier) {
        // TODO Check specific product identifiers
        return productSource.equals("sentinel2");
    }

    @Override
    public Resource getQuicklook(String productSource, String productIdentifier) throws IOException {
        try {
            return new PathResource(quicklooksCache.get(productIdentifier));
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Feature> transformCedaResult(SearchParameters parameters, CedaResult cedaResult) {
        return Optional.ofNullable(cedaResult.getRows()).map(rows -> rows.parallelStream()
                .map(row -> {
                    Feature f = new Feature();
                    f.setId((String) ((Map<String, Object>) (row.getMisc()).get("product_info")).get("Name"));
                    f.setGeometry(new Polygon(row.getSpatial().getGeometries().getDisplay().getCoordinates().get(0)));
                    f.setProperty("extraParams", row.getMisc());
                    setFtepProperties(parameters, f, row);
                    return f;
                })
                .collect(Collectors.toList()))
                .orElse(ImmutableList.of());
    }

    @SuppressWarnings("unchecked")
    private void setFtepProperties(SearchParameters parameters, Feature feature, CedaResultRow result) {
        String mission = (String) ((Map<String, Object>) (result.getMisc()).get("platform")).get("Mission");
        String productSource = SUPPORTED_MISSIONS.inverse().get(mission);
        String productIdentifier = feature.getId();
        URI ftepUri = externalProductService.getUri(productSource, productIdentifier);

        Set<Link> featureLinks = new HashSet<>();
        featureLinks.add(new Link(ftepUri.toASCIIString(), "ftep"));

        Long filesize;
        if (productSource.equals("landsat")) {
            // Sum the individual file sizes for landsat data
            filesize = Arrays.stream(result.getFile().getDataFileSizes().split(",")).mapToLong(Long::parseLong).sum();
        } else {
            filesize = result.getFile().getDataFileSize();
        }

        // Required parameters for FtepFile ingestion
        feature.setProperty("productSource", productSource);
        feature.setProperty("productIdentifier", productIdentifier);
        feature.setProperty("ftepUrl", ftepUri);
        feature.setProperty("filesize", filesize);
        feature.setProperty("ftepUsable", result.getFile().getLocation().equals("on_disk"));

        // Set "interesting" parameters which clients might want in an easily-accessible form
        // Some are not present depending on the result type, so we have to safely traverse the dynamic properties map
        // These are added to extraParams so that the FtepFile/Resto schema is predictable
        Map<String, Object> extraParams = (Map<String, Object>) feature.getProperties().get("extraParams");
        extraParams.put("ftepStartTime", result.getTemporal().getStartTime());
        extraParams.put("ftepEndTime", result.getTemporal().getEndTime());

        Optional.ofNullable((Map<String, Object>) result.getMisc().get("quality_info"))
                .map(qi -> qi.get("Cloud Coverage Assessment"))
                .ifPresent(cloudCoverage -> extraParams.put("ftepCloudCoverage", cloudCoverage));

        Optional.ofNullable((Map<String, Object>) result.getMisc().get("orbit_info"))
                .map(oi -> oi.get("Pass Direction"))
                .ifPresent(orbitDirection -> extraParams.put("ftepOrbitDirection", orbitDirection));

        Optional.ofNullable((Map<String, Object>) result.getMisc().get("product_info"))
                .map(pi -> pi.get("Product Type"))
                .ifPresent(productType -> extraParams.put("ftepProductType", productType));

        // CEDA only has quicklooks for S-2
        if (productSource.equals("sentinel2")) {
            HttpUrl.Builder quicklookUrlBuilder = parameters.getRequestUrl().newBuilder();
            parameters.getRequestUrl().queryParameterNames().forEach(quicklookUrlBuilder::removeAllQueryParameters);
            quicklookUrlBuilder.addPathSegment("ql").addPathSegment(productSource).addPathSegment(productIdentifier);
            featureLinks.add(new Link(quicklookUrlBuilder.build().toString(), "quicklook"));
        }

        feature.setProperty("_links", featureLinks.stream().collect(Collectors.toMap(
                Link::getRel,
                l -> ImmutableMap.of("href", l.getHref())
        )));
    }

    private SearchResults.Page getPageInfo(SearchParameters parameters, CedaResult cedaResult) {
        long totalResultsCount = cedaResult.getTotalResults();
        long itemsPerPage = parameters.getResultsPerPage();
        long startIndex = cedaResult.getStartIndex();
        long pageNumber = startIndex / itemsPerPage;
        long totalPages = (totalResultsCount + itemsPerPage - 1) / itemsPerPage;

        return SearchResults.Page.builder()
                .totalElements(totalResultsCount)
                .size(itemsPerPage)
                .number(pageNumber)
                .totalPages(totalPages)
                .build();
    }

    private Map<String, SearchResults.Link> getLinks(HttpUrl requestUrl, SearchResults.Page page, HttpUrl cedaUrl, CedaResult cedaResult) {
        Map<String, SearchResults.Link> links = new HashMap<>();

        links.putAll(getPagingLinks(page, requestUrl));
        links.put("upstreamSearch", SearchResults.Link.builder().rel("upstreamSearch").href(cedaUrl.toString()).build());

        return links;
    }

    private Map<String, SearchResults.Link> getPagingLinks(SearchResults.Page page, HttpUrl requestUrl) {
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

    private static String translateMissionParameter(String mission) {
        return SUPPORTED_MISSIONS.get(mission);
    }

    private static String translatePlatformParameter(String platform) {
        switch (platform) {
            case "S1A":
                return "Sentinel-1A";
            case "S1B":
                return "Sentinel-1B";
            case "S2A":
                return "Sentinel-2A";
            case "S3A":
                return "Sentinel-3A";
            default:
                return platform;
        }
    }

    private SearchResults.Link getPageLink(String rel, HttpUrl requestUrl, long page) {
        return SearchResults.Link.builder().rel(rel).href(requestUrl.newBuilder().removeAllQueryParameters("page").addQueryParameter("page", String.valueOf(page)).build().toString()).build();
    }
}
