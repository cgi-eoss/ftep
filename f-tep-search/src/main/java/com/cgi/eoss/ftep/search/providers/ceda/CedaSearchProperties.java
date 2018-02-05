package com.cgi.eoss.ftep.search.providers.ceda;

import lombok.Builder;
import lombok.Data;
import okhttp3.HttpUrl;

import java.net.URI;

@Data
@Builder
class CedaSearchProperties {

    private final HttpUrl baseUrl;
    private final URI ftpBaseUri;
    private final String username;
    private final String password;
    private final boolean usableProductsOnly;

}