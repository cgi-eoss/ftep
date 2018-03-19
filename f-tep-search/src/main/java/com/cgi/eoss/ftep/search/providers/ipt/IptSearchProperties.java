package com.cgi.eoss.ftep.search.providers.ipt;

import lombok.Builder;
import lombok.Data;
import okhttp3.HttpUrl;

@Data
@Builder
class IptSearchProperties {

    private final HttpUrl baseUrl;
    private final String username;
    private final String password;

}