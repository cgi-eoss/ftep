package com.cgi.eoss.ftep.search.api;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class SearchFacade {

    private final Set<SearchProvider> searchProviders;

    public SearchFacade(Collection<SearchProvider> searchProviders) {
        this.searchProviders = ImmutableSet.copyOf(searchProviders);
    }

    public SearchResults search(SearchParameters parameters) throws IOException {
        SearchProvider provider = getProvider(parameters);
        return provider.search(parameters);
    }

    private SearchProvider getProvider(SearchParameters parameters) {
        return searchProviders.stream()
                .filter(sp -> sp.supports(parameters.getRepo(), parameters))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No search providers found for parameters: " + parameters));
    }

}