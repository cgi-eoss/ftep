package com.cgi.eoss.ftep.search.api;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchFacade {

    private final Set<SearchProvider> searchProviders;

    public SearchFacade(Collection<SearchProvider> searchProviders) {
        this.searchProviders = ImmutableSet.copyOf(searchProviders);
    }

    public SearchResults search(Collection<FtepSearchParameter> parameters) throws IOException {
        Map<String, FtepSearchParameter> parameterMap = parameters.stream().collect(Collectors.toMap(FtepSearchParameter::getKey, p -> p));
        SearchProvider provider = getProvider(parameterMap);
        return provider.search(parameters);
    }

    private SearchProvider getProvider(Map<String, FtepSearchParameter> parameterMap) {
        RepoType repoType = (RepoType) parameterMap.get("repo").getValue();

        return searchProviders.stream()
                .filter(sp -> sp.supports(repoType, parameterMap))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No search providers found for parameters: " + parameterMap));
    }

}