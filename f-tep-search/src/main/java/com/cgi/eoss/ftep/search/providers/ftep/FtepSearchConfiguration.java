package com.cgi.eoss.ftep.search.providers.ftep;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.catalogue.resto.RestoService;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "ftep.search.ftep.enabled", havingValue = "true", matchIfMissing = true)
public class FtepSearchConfiguration {

    @Value("${ftep.search.ftep.baseUrl:http://ftep-resto}")
    private String baseUrl;
    @Value("${ftep.search.ftep.username:}")
    private String username;
    @Value("${ftep.search.ftep.password:}")
    private String password;

    @Bean
    public FtepSearchProvider ftepSearchProvider(OkHttpClient httpClient, ObjectMapper objectMapper, CatalogueService catalogueService, RestoService restoService, FtepFileDataService ftepFileDataService, FtepSecurityService securityService) {
        return new FtepSearchProvider(0,
                FtepSearchProperties.builder()
                        .baseUrl(HttpUrl.parse(baseUrl))
                        .username(username)
                        .password(password)
                        .build(),
                httpClient,
                objectMapper,
                catalogueService,
                restoService,
                ftepFileDataService,
                securityService);
    }

}
