package com.cgi.eoss.ftep.search.providers.creodias;

import lombok.Builder;
import lombok.Data;
import okhttp3.HttpUrl;

@Data
@Builder
class CreodiasSearchProperties {

    private final HttpUrl baseUrl;
    private final String username;
    private final String password;
    private final boolean usableProductsOnly;

}