package com.cgi.eoss.ftep.search.providers.scihub;

import lombok.Builder;
import lombok.Data;
import okhttp3.HttpUrl;

@Data
@Builder
public class SciHubSearchProperties {

    private final HttpUrl baseUrl;
    private final String username;
    private final String password;

}