package com.cgi.eoss.ftep.search.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface SearchProvider {
    SearchResults search(Collection<FtepSearchParameter> parameters) throws IOException;

    boolean supports(RepoType repoType, Map<String, FtepSearchParameter> parameterMap);
}