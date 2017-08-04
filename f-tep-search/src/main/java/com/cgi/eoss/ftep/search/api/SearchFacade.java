package com.cgi.eoss.ftep.search.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Log4j2
public class SearchFacade {

    private final List<SearchProvider> searchProviders;
    private final ObjectMapper yamlMapper;
    private final String parametersSchemaFile;

    public SearchFacade(Collection<SearchProvider> searchProviders, String parametersSchemaFile) throws IOException {
        this.searchProviders = ImmutableList.sortedCopyOf(searchProviders);
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.parametersSchemaFile = parametersSchemaFile;
    }

    public SearchResults search(SearchParameters parameters) throws IOException {
        SearchProvider provider = getProvider(parameters);
        return provider.search(parameters);
    }

    public Map<String, Object> getParametersSchema() throws IOException, URISyntaxException {
        InputStream parametersFile = Strings.isNullOrEmpty(parametersSchemaFile)
                ? getClass().getResourceAsStream("parameters.yaml")
                : Files.newInputStream(Paths.get(parametersSchemaFile));

        return yamlMapper.readValue(ByteStreams.toByteArray(parametersFile), new TypeReference<Map>() { });
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
}