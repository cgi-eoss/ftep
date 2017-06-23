package com.cgi.eoss.ftep.search.scihub;

import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SciHubSearchConfiguration {

    @Value("${ftep.search.scihub.baseUrl:https://scihub.copernicus.eu/apihub/search}")
    private String baseUrl;
    @Value("${ftep.search.scihub.username:}")
    private String username;
    @Value("${ftep.search.scihub.password:}")
    private String password;

    @Bean
    public SciHubSearchProvider sciHubSearchProvider() {
        return new SciHubSearchProvider(SciHubSearchProperties.builder()
                .baseUrl(HttpUrl.parse(baseUrl))
                .username(username)
                .password(password)
                .build());
    }

}
