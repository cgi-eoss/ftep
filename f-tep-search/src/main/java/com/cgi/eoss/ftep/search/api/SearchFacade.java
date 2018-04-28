package com.cgi.eoss.ftep.search.api;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.geojson.Feature;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Log4j2
public class SearchFacade {

    private final List<SearchProvider> searchProviders;
    private final ObjectMapper yamlMapper;
    private final String parametersSchemaFile;
    private final CatalogueService catalogueService;

    public SearchFacade(Collection<SearchProvider> searchProviders, String parametersSchemaFile, CatalogueService catalogueService) {
        this.searchProviders = ImmutableList.sortedCopyOf(searchProviders);
        this.catalogueService = catalogueService;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.parametersSchemaFile = parametersSchemaFile;
    }

    public SearchResults search(SearchParameters parameters) throws IOException {
        SearchProvider provider = getProvider(parameters);
        return provider.search(parameters);
    }

    public Map<String, Object> getParametersSchema() throws IOException {
        InputStream parametersFile = Strings.isNullOrEmpty(parametersSchemaFile)
                ? getClass().getResourceAsStream("parameters.yaml")
                : Files.newInputStream(Paths.get(parametersSchemaFile));

        return yamlMapper.readValue(ByteStreams.toByteArray(parametersFile), new TypeReference<Map>() {
        });
    }

    private SearchProvider getProvider(SearchParameters parameters) {
        return searchProviders.stream()
                .filter(sp -> sp.supports(parameters))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No search providers found for parameters: " + parameters));
    }

    public Resource getQuicklookResource(String productSource, String productIdentifier) throws IOException {
        SearchProvider provider = getQuicklooksProvider(productSource, productIdentifier);
        return provider.getQuicklook(productSource, productIdentifier);
    }

    private SearchProvider getQuicklooksProvider(String productSource, String productIdentifier) {
        return searchProviders.stream()
                .filter(sp -> sp.supportsQuicklook(productSource, productIdentifier))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No quicklook providers found for product: " + productIdentifier));
    }

    public Stream<FtepFile> findProducts(URI uri) {
        Stream<URI> uriStream = catalogueService.expand(uri);

        return uriStream
                .map(this::sanitiseUri)
                .flatMap(this::findProduct);
    }

    private Stream<FtepFile> findProduct(URI uri) {
        Optional<FtepFile> existing = catalogueService.get(uri);
        return existing.map(Stream::of)
                .orElseGet(() -> searchForAndCreateSatelliteProductReference(uri).map(Stream::of)
                        .orElse(Stream.empty()));
    }

    private URI sanitiseUri(URI uri) {
        // If the URI ends with .SAFE, trim it for consistency with older product URIs
        URI productUri;
        if (uri.toString().endsWith(".SAFE")) {
            productUri = URI.create(uri.toString().replace(".SAFE", ""));
        } else {
            productUri = uri;
        }
        return productUri;
    }

    private Optional<FtepFile> searchForAndCreateSatelliteProductReference(URI uri) {
        // Shortcut return things which are probably not identifiable products
        ImmutableSet<String> genericProtocols = ImmutableSet.of("http", "https", "file", "ftp", "ftep");
        if (Strings.isNullOrEmpty(uri.getPath()) || genericProtocols.contains(uri.getScheme())) {
            return Optional.empty();
        }

        try {
            String identifier = StringUtils.getFilename(uri.getPath())
                    .replace(".SAFE", "")
                    .replace(".zip", "");

            SearchParameters searchParameters = new SearchParameters();
            searchParameters.setRequestUrl(HttpUrl.parse("http://example.com"));
            searchParameters.setParameters(ImmutableListMultimap.<String, String>builder()
                    .put("catalogue", "SATELLITE")
                    .put("mission", uri.getScheme())
                    .put("identifier", identifier)
                    .build());

            List<Feature> features = search(searchParameters).getFeatures();

            if (features.size() != 1) {
                LOG.warn("Found zero or multiple products for URI {}", uri);
                return Optional.empty();
            } else {
                return Optional.of(catalogueService.indexExternalProduct(features.get(0)));
            }
        } catch (Exception e) {
            LOG.warn("Could not create FtepFile for URI {}", uri, e);
            return Optional.empty();
        }
    }

}