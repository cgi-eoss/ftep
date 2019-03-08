package com.cgi.eoss.ftep.search.providers.ipt;

import com.cgi.eoss.ftep.catalogue.external.ExternalProductDataService;
import com.cgi.eoss.ftep.search.api.SearchParameters;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.cgi.eoss.ftep.search.providers.resto.RestoResult;
import com.cgi.eoss.ftep.search.providers.resto.RestoSearchProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.geojson.Feature;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class IptSearchProvider extends RestoSearchProvider {

    @Data
    @Builder
    private static final class MissionPlatform {
        private final String mission;
        private final String platform;
    }

    private static final BiMap<MissionPlatform, String> SUPPORTED_MISSIONS = ImmutableBiMap.<MissionPlatform, String>builder()
            .put(MissionPlatform.builder().mission("envisat").platform(null).build(), "Envisat")
            .put(MissionPlatform.builder().mission("landsat").platform("Landsat-5").build(), "Landsat5")
            .put(MissionPlatform.builder().mission("landsat").platform("Landsat-7").build(), "Landsat7")
            .put(MissionPlatform.builder().mission("landsat").platform("Landsat-8").build(), "Landsat8")
            .put(MissionPlatform.builder().mission("sentinel1").platform(null).build(), "Sentinel1")
            .put(MissionPlatform.builder().mission("sentinel2").platform(null).build(), "Sentinel2")
            .put(MissionPlatform.builder().mission("sentinel3").platform(null).build(), "Sentinel3")
            .build();
    private static final Set<String> INTERNAL_FTEP_PARAMS = ImmutableSet.of(
            "catalogue",
            "mission",
            "platform"
    );
    private static final Map<String, String> PARAMETER_NAME_MAPPING = ImmutableMap.<String, String>builder()
            .put("semantic", "q")
            .put("aoi", "geometry")
            .put("s1ProcessingLevel", "processingLevel")
            .put("s2ProcessingLevel", "processingLevel")
            .put("s3ProcessingLevel", "processingLevel")
            .put("landsatProcessingLevel", "processingLevel")
            .put("s1ProductType", "productType")
            .put("productDateStart", "startDate")
            .put("productDateEnd", "completionDate")
            .put("maxCloudCover", "cloudCover")
            .put("identifier", "productIdentifier")
            .build();
    private static final Map<String, Function<String, String>> PARAMETER_VALUE_MAPPING = ImmutableMap.<String, Function<String, String>>builder()
            .put("identifier", v -> "%" + v + "%")
            .put("s1ProcessingLevel", v -> "LEVEL" + v)
            .put("s2ProcessingLevel", v -> "LEVELL" + v)
            .put("s3ProcessingLevel", v -> "LEVEL" + v)
            .put("landsatProcessingLevel", v -> "LEVEL" + v)
            .put("maxCloudCover", v -> "[0," + v + "]")
            .put("orbitDirection", String::toLowerCase)
            .build();

    private final int priority;
    private final ExternalProductDataService externalProductService;

    public IptSearchProvider(int priority, IptSearchProperties searchProperties, OkHttpClient httpClient, ObjectMapper objectMapper, ExternalProductDataService externalProductService) {
        super(searchProperties.getBaseUrl(),
                httpClient.newBuilder()
                        .addInterceptor(new HttpLoggingInterceptor(LOG::trace).setLevel(HttpLoggingInterceptor.Level.BODY))
                        .build(),
                objectMapper);
        this.priority = priority;
        this.externalProductService = externalProductService;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public Map<String, String> getQueryParameters(SearchParameters parameters) {
        Map<String, String> queryParameters = new HashMap<>();

        parameters.getParameters().asMap().entrySet().stream()
                .filter(p -> !INTERNAL_FTEP_PARAMS.contains(p.getKey()) && !p.getValue().isEmpty() && !Strings.isNullOrEmpty(Iterables.getFirst(p.getValue(), null)))
                .forEach(p -> addTransformedParameter(queryParameters, p));

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
        String mission = parameters.getValue("mission", "UNKNOWN");
        return catalogue.equals("SATELLITE") &&
                (SUPPORTED_MISSIONS.keySet().stream().map(MissionPlatform::getMission).anyMatch(m -> m.equals(mission)) || mission.equals("httpipt"));
    }

    @Override
    public boolean supportsQuicklook(String productSource, String productIdentifier) {
        return SUPPORTED_MISSIONS.keySet().stream().map(MissionPlatform::getMission).anyMatch(mission -> mission.equals(productSource));
    }

    @Override
    public Resource getQuicklook(String productSource, String productIdentifier) throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    protected Map<String, SearchResults.Link> getLinks(HttpUrl requestUrl, SearchResults.Page page, RestoResult restoResult) {
        Map<String, SearchResults.Link> links = new HashMap<>();

        links.putAll(getPagingLinks(page, requestUrl));

        return links;
    }

    @Override
    protected String getCollection(SearchParameters parameters) {
        MissionPlatform missionPlatform = getMissionPlatform(parameters);
        String mission = parameters.getValue("mission", "UNKNOWN");
        if (SUPPORTED_MISSIONS.containsKey(missionPlatform)) {
            return SUPPORTED_MISSIONS.get(missionPlatform);
        } else if (parameters.getValue("mission").isPresent()) {
            // We know it's here *somewhere*...
            return "";
        } else {
            throw new IllegalArgumentException("Could not identify IPT Resto collection for mission: " + missionPlatform);
        }
    }

    private MissionPlatform getMissionPlatform(SearchParameters parameters) {
        return MissionPlatform.builder()
                .mission(parameters.getValue("mission", "UNKNOWN"))
                .platform(parameters.getValue("platform", null))
                .build();
    }

    @Override
    protected SearchResults postProcess(SearchResults results) {
        results.getFeatures().forEach(f -> addFtepProperties(f, results.getParameters()));
        return results;
    }

    @SuppressWarnings("unchecked")
    private void addFtepProperties(Feature feature, SearchParameters parameters) {
        String collection = feature.getProperty("collection");
        String productSource = SUPPORTED_MISSIONS.inverse().get(collection).getMission();
        String productIdentifier = ((String) feature.getProperty("title")).replace(".SAFE", "");
        URI ftepUri = externalProductService.getUri(productSource, productIdentifier);

        // Shuffle the IPT properties into a sub-object for consistency across all search providers
        Map<String, Object> extraParams = new HashMap<>(feature.getProperties());
        feature.getProperties().clear();

        Set<Link> featureLinks = new HashSet<>();
        featureLinks.add(new Link(ftepUri.toASCIIString(), "ftep"));

        Long filesize = Optional.ofNullable((Map<String, Map<String, Object>>) extraParams.get("services"))
                .map(services -> Optional.ofNullable(services.get("download")).map(dl -> ((Number) dl.get("size")).longValue()).orElse(0L))
                .orElse(0L);

        // Required parameters for FtepFile ingestion
        feature.setProperty("productSource", productSource);
        feature.setProperty("productIdentifier", productIdentifier);
        feature.setProperty("ftepUrl", ftepUri);
        feature.setProperty("filesize", filesize);
        feature.setProperty("ftepUsable", true);

        // Set "interesting" parameters which clients might want in an easily-accessible form
        // Some are not present depending on the result type, so we have to safely traverse the dynamic properties map
        // These are added to extraParams so that the FtepFile/Resto schema is predictable
        Optional.ofNullable(extraParams.get("startDate"))
                .ifPresent(startDate -> extraParams.put("ftepStartTime", startDate));
        Optional.ofNullable(extraParams.get("completionDate"))
                .ifPresent(completionDate -> extraParams.put("ftepEndTime", completionDate));
        Optional.ofNullable(extraParams.get("cloudCover"))
                .ifPresent(cloudCoverage -> extraParams.put("ftepCloudCoverage", cloudCoverage));
        Optional.ofNullable(extraParams.get("orbitDirection"))
                .ifPresent(orbitDirection -> extraParams.put("ftepOrbitDirection", orbitDirection));
        Optional.ofNullable(extraParams.get("productType"))
                .ifPresent(productType -> extraParams.put("ftepProductType", productType));
        Optional.ofNullable(extraParams.get("updated"))
                .ifPresent(updated -> extraParams.put("ftepUpdated", updated));
        Optional.ofNullable(extraParams.get("published"))
                .ifPresent(published -> extraParams.put("ftepUpdated", published));
        feature.setProperty("extraParams", extraParams);

        // TODO Find an abstract quicklook representation
        Optional.ofNullable(extraParams.get("thumbnail"))
                .ifPresent(quicklookURL -> featureLinks.add(new Link(quicklookURL.toString(), "quicklook")));

        feature.setProperty("_links", featureLinks.stream().collect(Collectors.toMap(
                Link::getRel,
                l -> ImmutableMap.of("href", l.getHref())
        )));
    }
}
