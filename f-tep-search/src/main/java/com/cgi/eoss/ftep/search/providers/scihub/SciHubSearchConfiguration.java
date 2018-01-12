package com.cgi.eoss.ftep.search.providers.scihub;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "ftep.search.scihub.enabled", havingValue = "true")
public class SciHubSearchConfiguration {

    @Value("${ftep.search.scihub.baseUrl:https://scihub.copernicus.eu/apihub/search}")
    private String baseUrl;
    @Value("${ftep.search.scihub.username:}")
    private String username;
    @Value("${ftep.search.scihub.password:}")
    private String password;

    @Bean
    public SciHubSearchProvider sciHubSearchProvider(OkHttpClient httpClient) {
        return new SciHubSearchProvider(0,
                SciHubSearchProperties.builder()
                        .baseUrl(HttpUrl.parse(baseUrl))
                        .username(username)
                        .password(password)
                        .build(), httpClient);
    }

}
