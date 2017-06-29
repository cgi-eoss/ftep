package com.cgi.eoss.ftep.search.api;

import java.io.IOException;
import java.util.Map;

public interface SearchProvider {
    SearchResults search(SearchParameters parameters) throws IOException;

    Map<String, String> getPagingParameters(SearchParameters parameters);

    Map<String, String> getQueryParameters(SearchParameters parameters);

    boolean supports(RepoType repoType, SearchParameters parameters);
}