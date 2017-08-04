package com.cgi.eoss.ftep.search.providers.ftep;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.cgi.eoss.ftep.search.api.SearchParameters;
import com.cgi.eoss.ftep.search.api.SearchResults;
import com.cgi.eoss.ftep.search.providers.resto.RestoResult;
import com.cgi.eoss.ftep.search.providers.resto.RestoSearchProvider;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.Link;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class FtepSearchProvider extends RestoSearchProvider {

    private final int priority;
    private final CatalogueService catalogueService;
    private final RestoService restoService;
    private final FtepFileDataService ftepFileDataService;
    private final FtepSecurityService securityService;

    public FtepSearchProvider(int priority, FtepSearchProperties searchProperties, OkHttpClient httpClient, ObjectMapper objectMapper, CatalogueService catalogueService, RestoService restoService, FtepFileDataService ftepFileDataService, FtepSecurityService securityService) {
        super(searchProperties.getBaseUrl(),
                httpClient.newBuilder()
                        .addInterceptor(chain -> {
                            Request authenticatedRequest = chain.request().newBuilder()
                                    .header("Authorization", Credentials.basic(searchProperties.getUsername(), searchProperties.getPassword()))
                                    .build();
                            return chain.proceed(authenticatedRequest);
                        })
                        .addInterceptor(new HttpLoggingInterceptor(LOG::trace).setLevel(HttpLoggingInterceptor.Level.BODY))
                        .build(),
                objectMapper);
        this.priority = priority;
        this.catalogueService = catalogueService;
        this.restoService = restoService;
        this.ftepFileDataService = ftepFileDataService;
        this.securityService = securityService;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public Map<String, String> getQueryParameters(SearchParameters parameters) {
        Map<String, String> queryParameters = new HashMap<>();

        parameters.getValue("identifier").ifPresent(s -> queryParameters.put("productIdentifier", "%" + s + "%"));
        parameters.getValue("aoi").ifPresent(s -> queryParameters.put("geometry", s));
        parameters.getValue("owner").ifPresent(s -> queryParameters.put("owner", s));

        return queryParameters;
    }

    @Override
    public boolean supports(SearchParameters parameters) {
        String catalogue = parameters.getValue("catalogue", "UNKNOWN");
        return catalogue.equals("REF_DATA") || catalogue.equals("FTEP_PRODUCTS");
    }

    @Override
    public boolean supportsQuicklook(String productSource, String productIdentifier) {
        return false;
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
        switch (parameters.getValue("catalogue").orElse("")) {
            case "REF_DATA":
                return restoService.getReferenceDataCollection();
            case "FTEP_PRODUCTS":
                return restoService.getOutputProductsCollection();
            default:
                throw new IllegalArgumentException("Could not identify Resto collection for repo type: " + parameters.getValue("catalogue"));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SearchResults postProcess(SearchResults results) {
        SearchParameters parameters = results.getParameters();
        // Add visibility info, if the result can be matched to an FtepFile
        results.getFeatures().forEach(f -> {
            // Default to usable
            boolean ftepUsable = true;
            URI ftepUri = null;
            Long filesize = null;

            // TODO Migrate to Spring Data Rest somehow?
            Set<Link> featureLinks = new HashSet<>();

            try {
                FtepFile ftepFile = ftepFileDataService.getByRestoId(UUID.fromString(f.getId()));
                if (ftepFile != null) {
                    ftepUsable = securityService.isReadableByCurrentUser(FtepFile.class, ftepFile.getId());
                    ftepUri = ftepFile.getUri();
                    featureLinks.add(new Link(ftepUri.toASCIIString(), "ftep"));
                    filesize = ftepFile.getFilesize();

                    if (ftepUsable) {
                        HttpUrl.Builder downloadUrlBuilder = parameters.getRequestUrl().newBuilder();
                        parameters.getRequestUrl().queryParameterNames().forEach(downloadUrlBuilder::removeAllQueryParameters);
                        downloadUrlBuilder.addPathSegment("dl").addPathSegment("ftep").addPathSegment(String.valueOf(ftepFile.getId()));
                        featureLinks.add(new Link(downloadUrlBuilder.build().toString(), "download"));

                        HttpUrl wmsLink = catalogueService.getWmsUrl(ftepFile.getType(), ftepFile.getUri());
                        if (wmsLink != null) {
                            featureLinks.add(new Link(wmsLink.toString(), "wms"));
                        }
                    }
                } else {
                    LOG.debug("No FtepFile found for search result with ID: {}", f.getId());
                }
            } catch (Exception e) {
                LOG.debug("Could not check visibility of search result with ID: {}", f.getId(), e);
            }
            f.getProperties().put("ftepUsable", ftepUsable);
            f.getProperties().put("ftepUrl", ftepUri);
            f.getProperties().put("filesize", filesize);


            Map<String, Object> extraParams = Optional.ofNullable((Map<String, Object>) f.getProperties().get("extraParams")).orElse(new HashMap<>());

            if (results.getParameters().getValue("catalogue", "").equals("REF_DATA")) {
                // Reference data timestamp is just the publish time
                extraParams.put("ftepStartTime", ZonedDateTime.parse(f.getProperty("published")).with(ZoneOffset.UTC).toLocalDateTime());
                extraParams.put("ftepEndTime", ZonedDateTime.parse(f.getProperty("published")).with(ZoneOffset.UTC).toLocalDateTime());
            } else if (results.getParameters().getValue("catalogue", "").equals("FTEP_PRODUCTS")) {
                // F-TEP products should have jobStart/EndDate
                extraParams.put("ftepStartTime", ZonedDateTime.parse(f.getProperty("jobStartDate")).with(ZoneOffset.UTC).toLocalDateTime());
                extraParams.put("ftepEndTime", ZonedDateTime.parse(f.getProperty("jobEndDate")).with(ZoneOffset.UTC).toLocalDateTime());
            }

            f.getProperties().put("extraParams", extraParams);

            // F-TEP links are "_links", resto links are "links"
            f.setProperty("_links", featureLinks.stream().collect(Collectors.toMap(
                    Link::getRel,
                    l -> ImmutableMap.of("href", l.getHref())
            )));
        });
        return results;
    }

}
