package com.cgi.eoss.ftep.search.ftep;

import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FtepRestoSearchConfiguration {

    @Value("${ftep.search.resto.baseUrl:http://ftep-resto/resto}")
    private String baseUrl;
    @Value("${ftep.search.resto.username:}")
    private String username;
    @Value("${ftep.search.resto.password:}")
    private String password;

    @Bean
    public FtepRestoSearchProvider restoSearchProvider(OkHttpClient httpClient, ObjectMapper objectMapper, RestoService restoService, FtepFileDataService ftepFileDataService) {
        return new FtepRestoSearchProvider(
                FtepSearchProperties.builder()
                        .baseUrl(HttpUrl.parse(baseUrl))
                        .username(username)
                        .password(password)
                        .build(),
                httpClient,
                objectMapper,
                restoService,
                ftepFileDataService);
    }

}
